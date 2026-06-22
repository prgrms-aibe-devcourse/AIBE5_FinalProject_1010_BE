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
import java.util.Objects;

/**
 * 강의실 듣기 자료(오디오)의 "권위 있는 현재 재생 상태"를 세션별로 Redis에 보관한다 (이슈 #182, 멀티 인스턴스 대응).
 *
 * <p>화이트보드({@link WhiteboardStateStore})와 같은 방식: 상태를 Redis에 JSON으로 저장하고 read-modify-write를
 * 세션 단위 분산 락({@link RedisStateLock})으로 직렬화한다. 선생님(호스트)이 보낸 제어를 반영 후 전원에게 재방송하며,
 * 늦은 입장자는 REST 스냅샷으로 재생목록·트랙·재생여부·위치·배속·반복을 복원한다. 방치 상태는 TTL로 자동 만료.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AudioStateStore {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final RedisStateLock lock;

    private static final double MIN_RATE = 0.2;
    private static final double MAX_RATE = 3.0;
    private static final Duration TTL = Duration.ofHours(24);

    private String key(Long sessionId) { return "audio:" + sessionId; }
    private String lockName(Long sessionId) { return "audio:" + sessionId; }

    /** 한 세션의 오디오 재생 컨텍스트(작업용 인메모리 표현 — 영속은 Redis JSON). */
    static final class Audio {
        Long hostUserId;
        final List<Map<String, Object>> playlist = new ArrayList<>();
        String url;
        String fileName;
        Long fileId;
        boolean playing;
        double positionSec;
        long updatedAtMs;
        double rate = 1.0;
        boolean loopOn;
        double loopStart;
        double loopEnd;
    }

    /** 호스트(제어 권한자) 등록 — 입장 시 호출. */
    public void setHost(Long sessionId, Long userId) {
        lock.withLock(lockName(sessionId), () -> {
            Audio a = load(sessionId);
            a.hostUserId = userId;
            save(sessionId, a);
            return null;
        });
    }

    public boolean isHost(Long sessionId, Long userId) {
        Audio a = load(sessionId);
        return a.hostUserId != null && userId != null && a.hostUserId.equals(userId);
    }

    /** 재생목록에 트랙 추가(같은 fileId면 무시). */
    public void add(Long sessionId, String url, String fileName, Long fileId) {
        if (url == null) return;
        lock.withLock(lockName(sessionId), () -> {
            Audio a = load(sessionId);
            boolean exists = a.playlist.stream().anyMatch(t -> idEq(fileId, t.get("fileId")));
            if (!exists) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("url", url);
                t.put("fileName", fileName);
                t.put("fileId", fileId);
                a.playlist.add(t);
                save(sessionId, a);
            }
            return null;
        });
    }

    /**
     * 재생목록에서 트랙 선택. 목록에 없으면 false(호출측이 재방송하지 않도록).
     */
    public boolean select(Long sessionId, Long fileId) {
        return lock.withLock(lockName(sessionId), () -> {
            Audio a = load(sessionId);
            Map<String, Object> t = a.playlist.stream()
                    .filter(x -> idEq(fileId, x.get("fileId")))
                    .findFirst().orElse(null);
            if (t == null) return false;
            a.url = str(t.get("url"));
            a.fileName = str(t.get("fileName"));
            a.fileId = fileId;
            a.playing = false;
            a.positionSec = 0;
            a.loopOn = false;
            a.loopStart = 0;
            a.loopEnd = 0;
            a.updatedAtMs = now();
            save(sessionId, a);
            return true;
        });
    }

    /** 재생목록에서 트랙 삭제. 현재 트랙이면 재생 정지하고 현재 트랙 해제. */
    public void removeTrack(Long sessionId, Long fileId) {
        lock.withLock(lockName(sessionId), () -> {
            Audio a = load(sessionId);
            a.playlist.removeIf(t -> idEq(fileId, t.get("fileId")));
            if (Objects.equals(fileId, a.fileId)) {
                a.url = null;
                a.fileName = null;
                a.fileId = null;
                a.playing = false;
                a.positionSec = 0;
                a.loopOn = false;
                a.updatedAtMs = now();
            }
            save(sessionId, a);
            return null;
        });
    }

    public void play(Long sessionId, double positionSec) {
        mutate(sessionId, a -> { a.playing = true; a.positionSec = Math.max(0, positionSec); a.updatedAtMs = now(); });
    }

    public void pause(Long sessionId, double positionSec) {
        mutate(sessionId, a -> { a.playing = false; a.positionSec = Math.max(0, positionSec); a.updatedAtMs = now(); });
    }

    public void seek(Long sessionId, double positionSec, Boolean playing) {
        mutate(sessionId, a -> {
            a.positionSec = Math.max(0, positionSec);
            if (playing != null) a.playing = playing;
            a.updatedAtMs = now();
        });
    }

    public void stop(Long sessionId) {
        mutate(sessionId, a -> { a.playing = false; a.positionSec = 0; a.updatedAtMs = now(); });
    }

    /** 배속 변경 — 클라가 보낸 현재 위치로 기준 재설정. */
    public void setRate(Long sessionId, double rate, double positionSec) {
        mutate(sessionId, a -> {
            a.rate = Math.max(MIN_RATE, Math.min(MAX_RATE, rate <= 0 ? 1.0 : rate));
            a.positionSec = Math.max(0, positionSec);
            a.updatedAtMs = now();
        });
    }

    /** AB 반복 구간 설정. */
    public void setLoop(Long sessionId, boolean on, double start, double end) {
        mutate(sessionId, a -> {
            a.loopOn = on;
            a.loopStart = Math.max(0, start);
            a.loopEnd = Math.max(0, end);
        });
    }

    /** 입장/재동기화용 스냅샷. 재생 중이면 배속 반영 경과시간을 더해 현재 위치를 계산. */
    public Map<String, Object> snapshot(Long sessionId) {
        Audio a = load(sessionId);
        Map<String, Object> out = new LinkedHashMap<>();
        long now = now();
        out.put("serverNowMs", now);
        out.put("playlist", new ArrayList<>(a.playlist));
        out.put("rate", a.rate);
        out.put("loopOn", a.loopOn);
        out.put("loopStart", a.loopStart);
        out.put("loopEnd", a.loopEnd);
        if (a.url == null) {
            out.put("url", null);
            out.put("playing", false);
            out.put("positionSec", 0.0);
            return out;
        }
        double effective = a.playing ? a.positionSec + (now - a.updatedAtMs) / 1000.0 * a.rate : a.positionSec;
        out.put("url", a.url);
        out.put("fileName", a.fileName);
        out.put("fileId", a.fileId);
        out.put("playing", a.playing);
        out.put("positionSec", Math.max(0, effective));
        out.put("updatedAtMs", a.updatedAtMs);
        return out;
    }

    public void clear(Long sessionId) {
        redisTemplate.delete(key(sessionId));
    }

    // ───────────── 공통 mutate(락) + Redis load/save ─────────────

    private void mutate(Long sessionId, java.util.function.Consumer<Audio> fn) {
        lock.withLock(lockName(sessionId), () -> {
            Audio a = load(sessionId);
            fn.accept(a);
            save(sessionId, a);
            return null;
        });
    }

    private Audio load(Long sessionId) {
        String json = redisTemplate.opsForValue().get(key(sessionId));
        if (json == null) return new Audio();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> m = objectMapper.readValue(json, Map.class);
            return fromMap(m);
        } catch (Exception e) {
            log.warn("[audio] 상태 역직렬화 실패 — 빈 상태로 시작 (sessionId={})", sessionId, e);
            return new Audio();
        }
    }

    private void save(Long sessionId, Audio a) {
        try {
            redisTemplate.opsForValue().set(key(sessionId), objectMapper.writeValueAsString(rawMap(a)), TTL);
        } catch (Exception e) {
            log.warn("[audio] 상태 직렬화 저장 실패 (sessionId={})", sessionId, e);
        }
    }

    private Map<String, Object> rawMap(Audio a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("hostUserId", a.hostUserId);
        m.put("playlist", a.playlist);
        m.put("url", a.url);
        m.put("fileName", a.fileName);
        m.put("fileId", a.fileId);
        m.put("playing", a.playing);
        m.put("positionSec", a.positionSec);
        m.put("updatedAtMs", a.updatedAtMs);
        m.put("rate", a.rate);
        m.put("loopOn", a.loopOn);
        m.put("loopStart", a.loopStart);
        m.put("loopEnd", a.loopEnd);
        return m;
    }

    @SuppressWarnings("unchecked")
    private Audio fromMap(Map<String, Object> m) {
        Audio a = new Audio();
        a.hostUserId = longVal(m.get("hostUserId"));
        Object pl = m.get("playlist");
        if (pl instanceof List<?> list) {
            for (Object o : list) if (o instanceof Map) a.playlist.add((Map<String, Object>) o);
        }
        a.url = str(m.get("url"));
        a.fileName = str(m.get("fileName"));
        a.fileId = longVal(m.get("fileId"));
        a.playing = boolVal(m.get("playing"));
        a.positionSec = dblVal(m.get("positionSec"));
        Long u = longVal(m.get("updatedAtMs"));
        a.updatedAtMs = u != null ? u : 0L;
        a.rate = m.get("rate") != null ? dblVal(m.get("rate")) : 1.0;
        a.loopOn = boolVal(m.get("loopOn"));
        a.loopStart = dblVal(m.get("loopStart"));
        a.loopEnd = dblVal(m.get("loopEnd"));
        return a;
    }

    private static long now() {
        return System.currentTimeMillis();
    }

    /** JSON 라운드트립으로 Integer/Long이 섞여도 숫자 값으로 비교(fileId 매칭 안정성). */
    private static boolean idEq(Long target, Object stored) {
        return target != null && target.equals(longVal(stored));
    }

    private static Long longVal(Object o) {
        if (o instanceof Number n) return n.longValue();
        if (o == null) return null;
        try { return Long.valueOf(String.valueOf(o)); } catch (NumberFormatException e) { return null; }
    }

    private static double dblVal(Object o) {
        if (o instanceof Number n) return n.doubleValue();
        if (o == null) return 0;
        try {
            double v = Double.parseDouble(String.valueOf(o));
            return Double.isFinite(v) ? v : 0;
        } catch (NumberFormatException e) { return 0; }
    }

    private static boolean boolVal(Object o) {
        return Boolean.TRUE.equals(o) || "true".equalsIgnoreCase(String.valueOf(o));
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
