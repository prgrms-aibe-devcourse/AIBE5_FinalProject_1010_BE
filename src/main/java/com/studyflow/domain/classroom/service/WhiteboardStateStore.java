package com.studyflow.domain.classroom.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 강의실 화이트보드의 "권위 있는(authoritative) 현재 상태"를 세션별로 메모리에 보관한다 (이슈 #131).
 *
 * <p>서버가 단일 진실원천(single source of truth)이다. 클라이언트는 변경 의도(op)만 보내고,
 * 서버가 이 상태에 반영한 뒤 단조 증가하는 순번(seq)을 붙여 전원에게 재방송한다.
 * 모든 클라이언트가 같은 seq 순서로 같은 op를 적용하므로 화면이 분기(divergence)하지 않는다.
 * 메시지가 유실/재정렬되면 클라이언트는 seq 구멍을 보고 REST로 전체 상태를 다시 받아 자가 치유한다.</p>
 *
 * <p>도형의 "종류"(펜·사각형·텍스트…)는 해석하지 않는다. 도형은 그저 "id를 가진 Map"이며,
 * 서버는 페이지별 도형 목록의 추가/수정/삭제/순서만 generic 하게 관리한다(도구가 늘어도 서버 수정 0줄).
 * 단일 인스턴스 in-memory — 다중 인스턴스 확장 시 Redis 등으로 대체.</p>
 */
@Component
public class WhiteboardStateStore {

    /** 한 페이지(= shapes의 순서 있는 목록). */
    static final class Page {
        final String id;
        List<Map<String, Object>> shapes = new ArrayList<>();
        Page(String id) { this.id = id; }
    }

    /** 한 세션의 보드(페이지 목록 + 적용된 op 순번 + 전원이 함께 보는 활성 페이지). */
    static final class Board {
        final List<Page> pages = new ArrayList<>();
        long seq = 0;
        String activePageId = "p1"; // 모두가 같은 페이지를 보도록 — 판서 권한자가 이동하면 전파(이슈)
        Board() { pages.add(new Page("p1")); } // 모든 세션은 공통 첫 페이지 "p1"로 시작
    }

    private final Map<Long, Board> boards = new ConcurrentHashMap<>();

    private Board board(Long sessionId) {
        return boards.computeIfAbsent(sessionId, k -> new Board());
    }

    /**
     * op 배치를 보드에 적용하고 새 순번(seq)을 반환한다. 세션 보드 단위로 직렬화(원자적)된다.
     * 메시지 1건(= ops 배치)이 순번 1개의 단위다.
     */
    public long apply(Long sessionId, List<Map<String, Object>> ops) {
        Board b = board(sessionId);
        synchronized (b) {
            // 변경할 op가 없으면 seq를 올리지 않는다 — 빈 메시지로 순번이 낭비되거나
            // 수신측이 구멍으로 오인해 불필요한 resync를 하지 않도록 현재 seq를 그대로 돌려준다.
            if (ops == null || ops.isEmpty()) {
                return b.seq;
            }
            for (Map<String, Object> op : ops) applyOne(b, op);
            return ++b.seq;
        }
    }

    /** 입장/재동기화용 전체 상태 스냅샷: {pages:[{id,shapes}], seq}. */
    public Map<String, Object> snapshot(Long sessionId) {
        Board b = board(sessionId);
        synchronized (b) {
            List<Map<String, Object>> pages = new ArrayList<>();
            for (Page p : b.pages) {
                Map<String, Object> pm = new LinkedHashMap<>();
                pm.put("id", p.id);
                pm.put("shapes", new ArrayList<>(p.shapes)); // 얕은 복사(전송용)
                pages.add(pm);
            }
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("pages", pages);
            out.put("seq", b.seq);
            out.put("activePageId", b.activePageId); // 입장/재동기화 시 현재 활성 페이지로 맞추도록
            return out;
        }
    }

    /** 전원이 함께 볼 활성 페이지 변경(판서 권한자가 페이지 이동 시). */
    public void setActivePage(Long sessionId, String pageId) {
        if (pageId == null) return;
        Board b = board(sessionId);
        synchronized (b) { b.activePageId = pageId; }
    }

    public void clear(Long sessionId) {
        boards.remove(sessionId);
    }

    // ───────────── op 적용 (generic) ─────────────

    private void applyOne(Board b, Map<String, Object> op) {
        String type = str(op.get("op"));
        if (type == null) return;
        String pageId = str(op.get("pageId"));

        if ("addPage".equals(type)) {
            if (pageId != null && findPage(b, pageId) == null) b.pages.add(new Page(pageId));
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
                    // 부분 갱신: 기존 Map을 "제자리 수정"하지 않고 새 Map으로 교체한다.
                    // snapshot()이 얕은 복사로 같은 Map 참조를 들고 직렬화하는 동안 이 Map을
                    // 다른 스레드가 수정하면 ConcurrentModificationException이 날 수 있으므로,
                    // 교체 방식으로 기존 Map을 불변으로 유지한다.
                    Map<String, Object> merged = new LinkedHashMap<>(p.shapes.get(idx));
                    patch.forEach((k, v) -> { if (v != null) merged.put(k, v); }); // 들어온 필드만(없는 src 등 보존)
                    p.shapes.set(idx, merged);
                } else {
                    p.shapes.add(patch); // 없으면 추가(견고성)
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
                reordered.addAll(byId.values()); // 목록에 없던 것 보존
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

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return (o instanceof Map) ? (Map<String, Object>) o : null;
    }
}
