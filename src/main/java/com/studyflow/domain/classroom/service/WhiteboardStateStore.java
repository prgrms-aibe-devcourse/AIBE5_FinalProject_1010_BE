package com.studyflow.domain.classroom.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.global.redis.RedisStateLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 강의실 화이트보드의 "권위 있는(authoritative) 현재 상태"를 세션별로 Redis에 보관한다 (이슈 #131, 멀티 인스턴스 대응).
 *
 * <p>서버가 단일 진실원천이다. 클라이언트는 변경 의도(op)만 보내고, 서버가 이 상태에 반영한 뒤 단조 증가하는
 * 순번(seq)을 붙여 전원에게 재방송한다. 멀티 인스턴스에서 보드를 공유하기 위해 Redis에 JSON으로 저장하고,
 * read-modify-write는 세션 단위 분산 락({@link RedisStateLock})으로 직렬화한다(인스턴스 간 lost update 방지).</p>
 *
 * <p>도형의 "종류"는 해석하지 않는다. 도형은 "id를 가진 Map"이며, 페이지별 도형 목록의 추가/수정/삭제/순서만
 * generic 하게 관리한다. 방치된 보드는 TTL로 자동 만료된다(종료 시 clear가 누락돼도 누적되지 않도록).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WhiteboardStateStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStateLock lock;

    private static final Duration TTL = Duration.ofHours(24);

    private String key(Long sessionId) { return "wb:board:" + sessionId; }
    private String lockName(Long sessionId) { return "wb:" + sessionId; }

    /** 한 페이지(= shapes의 순서 있는 목록). */
    static final class Page {
        final String id;
        String kind = "board";
        String pdfDocId;
        Integer pdfPage;
        Integer pdfPageCount;
        String pdfSrc;
        String fileName;
        List<Map<String, Object>> shapes = new ArrayList<>();
        Page(String id) { this.id = id; }
    }

    /** 한 세션의 보드(페이지 목록 + 적용된 op 순번 + 전원이 함께 보는 활성 페이지). */
    static final class Board {
        final List<Page> pages = new ArrayList<>();
        long seq = 0;
        String activePageId = "p1";
        Board() { pages.add(new Page("p1")); } // 모든 세션은 공통 첫 페이지 "p1"로 시작
    }

    /**
     * op 배치를 보드에 적용하고 새 순번(seq)을 반환한다. 세션 단위 락으로 직렬화(원자적)된다.
     * 변경할 op가 없으면 seq를 올리지 않고 현재 seq를 그대로 돌려준다.
     */
    public long apply(Long sessionId, List<Map<String, Object>> ops) {
        if (ops == null || ops.isEmpty()) {
            return load(sessionId).seq;
        }
        return lock.withLock(lockName(sessionId), () -> {
            Board b = load(sessionId);
            for (Map<String, Object> op : ops) applyOne(b, op);
            long seq = ++b.seq;
            save(sessionId, b);
            return seq;
        });
    }

    /** 입장/재동기화용 전체 상태 스냅샷: {pages, seq, activePageId}. */
    public Map<String, Object> snapshot(Long sessionId) {
        // 저장이 JSON 문자열 단위 원자적(SET)이라, 락 없이 읽어도 부분 상태를 보지 않는다.
        return toMap(load(sessionId));
    }

    /** 전원이 함께 볼 활성 페이지 변경(판서 권한자가 페이지 이동 시). */
    public void setActivePage(Long sessionId, String pageId) {
        if (pageId == null) return;
        lock.withLock(lockName(sessionId), () -> {
            Board b = load(sessionId);
            b.activePageId = pageId;
            save(sessionId, b);
            return null;
        });
    }

    public void clear(Long sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    // ───────────── Redis load/save (JSON) ─────────────

    private Board load(Long sessionId) {
        String json = redisTemplate.opsForValue().get(key(sessionId));
        if (json == null) return new Board();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(json, Map.class);
            return fromMap(m);
        } catch (Exception e) {
            log.warn("[whiteboard] 보드 역직렬화 실패 — 빈 보드로 시작 (sessionId={})", sessionId, e);
            return new Board();
        }
    }

    private void save(Long sessionId, Board b) {
        try {
            redisTemplate.opsForValue().set(key(sessionId), objectMapper.writeValueAsString(toMap(b)), TTL);
        } catch (Exception e) {
            log.warn("[whiteboard] 보드 직렬화 저장 실패 (sessionId={})", sessionId, e);
        }
    }

    private Map<String, Object> toMap(Board b) {
        List<Map<String, Object>> pages = new ArrayList<>();
        for (Page p : b.pages) {
            Map<String, Object> pm = new LinkedHashMap<>();
            pm.put("id", p.id);
            pm.put("kind", p.kind);
            pm.put("pdfDocId", p.pdfDocId);
            pm.put("pdfPage", p.pdfPage);
            pm.put("pdfPageCount", p.pdfPageCount);
            pm.put("pdfSrc", p.pdfSrc);
            pm.put("fileName", p.fileName);
            pm.put("shapes", new ArrayList<>(p.shapes));
            pages.add(pm);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("pages", pages);
        out.put("seq", b.seq);
        out.put("activePageId", b.activePageId);
        return out;
    }

    @SuppressWarnings("unchecked")
    private Board fromMap(Map<String, Object> m) {
        Board b = new Board();
        b.pages.clear(); // 기본 p1 제거 후 저장된 페이지로 재구성
        Object pagesObj = m.get("pages");
        if (pagesObj instanceof List<?> pages) {
            for (Object po : pages) {
                if (!(po instanceof Map)) continue;
                Map<String, Object> pm = (Map<String, Object>) po;
                Page p = new Page(str(pm.get("id")));
                p.kind = pm.get("kind") != null ? str(pm.get("kind")) : "board";
                p.pdfDocId = str(pm.get("pdfDocId"));
                p.pdfPage = intVal(pm.get("pdfPage"));
                p.pdfPageCount = intVal(pm.get("pdfPageCount"));
                p.pdfSrc = str(pm.get("pdfSrc"));
                p.fileName = str(pm.get("fileName"));
                Object shapesObj = pm.get("shapes");
                if (shapesObj instanceof List<?> shapes) {
                    for (Object so : shapes) if (so instanceof Map) p.shapes.add((Map<String, Object>) so);
                }
                b.pages.add(p);
            }
        }
        if (b.pages.isEmpty()) b.pages.add(new Page("p1")); // 안전장치
        Object seq = m.get("seq");
        if (seq instanceof Number n) b.seq = n.longValue();
        Object active = m.get("activePageId");
        if (active != null) b.activePageId = str(active);
        return b;
    }

    // ───────────── op 적용 (generic) ─────────────

    private void applyOne(Board b, Map<String, Object> op) {
        String type = str(op.get("op"));
        if (type == null) return;
        String pageId = str(op.get("pageId"));

        if ("addPage".equals(type)) {
            if (pageId != null && findPage(b, pageId) == null) {
                Page p = new Page(pageId);
                p.kind = str(op.get("kind")) != null ? str(op.get("kind")) : "board";
                p.pdfDocId = str(op.get("pdfDocId"));
                p.pdfPage = intVal(op.get("pdfPage"));
                p.pdfPageCount = intVal(op.get("pdfPageCount"));
                p.pdfSrc = str(op.get("pdfSrc"));
                p.fileName = str(op.get("fileName"));
                b.pages.add(p);
            }
            return;
        }
        if ("removePage".equals(type)) {
            b.pages.removeIf(p -> p.id.equals(pageId));
            return;
        }

        if (pageId == null) return;
        Page p = findPage(b, pageId);
        if (p == null) { // addPage 유실 대비 견고성 — 페이지가 없으면 만들어 받아준다
            p = new Page(pageId);
            b.pages.add(p);
        }

        switch (type) {
            case "add": {
                Map<String, Object> shape = asMap(op.get("shape"));
                if (shape == null) return;
                if (findShape(p, str(shape.get("id"))) == null) p.shapes.add(shape);
                return;
            }
            case "update": {
                Map<String, Object> patch = asMap(op.get("shape"));
                if (patch == null) return;
                int idx = indexOfShape(p, str(patch.get("id")));
                if (idx >= 0) {
                    Map<String, Object> merged = new LinkedHashMap<>(p.shapes.get(idx));
                    patch.forEach((k, v) -> { if (v != null) merged.put(k, v); });
                    p.shapes.set(idx, merged);
                } else {
                    p.shapes.add(patch);
                }
                return;
            }
            case "remove": {
                String id = str(op.get("id"));
                if (id != null) p.shapes.removeIf(s -> id.equals(str(s.get("id"))));
                return;
            }
            case "clear":
                p.shapes.clear();
                return;
            case "reorder": {
                Object idsObj = op.get("ids");
                if (!(idsObj instanceof List)) return;
                List<?> ids = (List<?>) idsObj;
                Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
                for (Map<String, Object> s : p.shapes) byId.put(str(s.get("id")), s);
                List<Map<String, Object>> reordered = new ArrayList<>();
                for (Object idObj : ids) {
                    Map<String, Object> s = byId.remove(str(idObj));
                    if (s != null) reordered.add(s);
                }
                reordered.addAll(byId.values());
                p.shapes = reordered;
                return;
            }
            default:
                // 알 수 없는 op는 무시
        }
    }

    private static Page findPage(Board b, String pageId) {
        if (pageId == null) return null;
        for (Page p : b.pages) if (p.id.equals(pageId)) return p;
        return null;
    }

    private static Map<String, Object> findShape(Page p, String id) {
        if (id == null) return null;
        for (Map<String, Object> s : p.shapes) if (id.equals(str(s.get("id")))) return s;
        return null;
    }

    private static int indexOfShape(Page p, String id) {
        if (id == null) return -1;
        for (int i = 0; i < p.shapes.size(); i++) if (id.equals(str(p.shapes.get(i).get("id")))) return i;
        return -1;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static Integer intVal(Object o) {
        if (o == null) return null;
        try {
            return Integer.valueOf(String.valueOf(o));
        } catch (Exception ignore) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }
}
