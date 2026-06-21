package com.studyflow.domain.classroom.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 강의실 듣기 자료(오디오)의 "권위 있는 현재 재생 상태"를 세션별로 메모리에 보관한다 (이슈 #182).
 *
 * <p>화이트보드({@link WhiteboardStateStore})와 같은 철학: 서버가 단일 진실원천이다. 선생님(호스트)이
 * 보낸 제어(add/select/removeTrack/play/pause/seek/stop/rate/loop)를 이 상태에 반영한 뒤 같은 세션
 * 전원에게 재방송한다. 늦게 들어온 참가자는 REST 스냅샷으로 재생목록·현재 트랙·재생여부·위치·배속·반복구간을 복원한다.</p>
 *
 * <p>오디오 "제어"는 호스트(수업 진행 선생님)만 가능하다 — 입장 시 {@code setHost}로 기록하고
 * WebSocket 컨트롤러가 {@code isHost}로 게이팅한다. 단일 인스턴스 in-memory.</p>
 */
@Component
public class AudioStateStore {

    /** 한 세션의 오디오 재생 컨텍스트. */
    static final class Audio {
        Long hostUserId;                 // 오디오 제어 권한자(수업 진행 선생님)
        final List<Map<String, Object>> playlist = new ArrayList<>(); // [{url, fileName, fileId}] 업로드 순
        String url;                      // 현재 트랙 URL (null = 선택된 트랙 없음)
        String fileName;
        Long fileId;
        boolean playing;
        double positionSec;              // updatedAtMs 기준 시점의 재생 위치(초)
        long updatedAtMs;                // 위 상태가 설정된 서버 epoch ms
        double rate = 1.0;               // 재생 배속(0.2~3.0)
        boolean loopOn;                  // AB 반복 사용 여부
        double loopStart;                // 반복 시작(초)
        double loopEnd;                  // 반복 끝(초)
    }

    private static final double MIN_RATE = 0.2;
    private static final double MAX_RATE = 3.0;

    private final Map<Long, Audio> audios = new ConcurrentHashMap<>();

    private Audio audio(Long sessionId) {
        return audios.computeIfAbsent(sessionId, k -> new Audio());
    }

    /** 호스트(제어 권한자) 등록 — 입장 시 호출. */
    public void setHost(Long sessionId, Long userId) {
        Audio a = audio(sessionId);
        synchronized (a) { a.hostUserId = userId; }
    }

    public boolean isHost(Long sessionId, Long userId) {
        Audio a = audios.get(sessionId);
        return a != null && userId != null && userId.equals(a.hostUserId);
    }

    /** 재생목록에 트랙 추가(같은 fileId면 무시). */
    public void add(Long sessionId, String url, String fileName, Long fileId) {
        if (url == null) return;
        Audio a = audio(sessionId);
        synchronized (a) {
            boolean exists = a.playlist.stream().anyMatch(t -> fileId != null && fileId.equals(t.get("fileId")));
            if (!exists) {
                Map<String, Object> t = new LinkedHashMap<>();
                t.put("url", url);
                t.put("fileName", fileName);
                t.put("fileId", fileId);
                a.playlist.add(t);
            }
        }
    }

    /** 재생목록에서 트랙 선택(현재 트랙 = 그 트랙, 위치 0·정지·반복 해제). */
    public void select(Long sessionId, Long fileId) {
        Audio a = audio(sessionId);
        synchronized (a) {
            Map<String, Object> t = a.playlist.stream()
                    .filter(x -> fileId != null && fileId.equals(x.get("fileId")))
                    .findFirst().orElse(null);
            if (t == null) return;
            a.url = (String) t.get("url");
            a.fileName = (String) t.get("fileName");
            a.fileId = fileId;
            a.playing = false;
            a.positionSec = 0;
            a.loopOn = false;
            a.loopStart = 0;
            a.loopEnd = 0;
            a.updatedAtMs = now();
        }
    }

    /** 재생목록에서 트랙 삭제. 현재 트랙이면 재생 정지하고 현재 트랙 해제. */
    public void removeTrack(Long sessionId, Long fileId) {
        Audio a = audio(sessionId);
        synchronized (a) {
            a.playlist.removeIf(t -> fileId != null && fileId.equals(t.get("fileId")));
            if (fileId != null && fileId.equals(a.fileId)) {
                a.url = null;
                a.fileName = null;
                a.fileId = null;
                a.playing = false;
                a.positionSec = 0;
                a.loopOn = false;
                a.updatedAtMs = now();
            }
        }
    }

    public void play(Long sessionId, double positionSec) {
        Audio a = audio(sessionId);
        synchronized (a) { a.playing = true; a.positionSec = Math.max(0, positionSec); a.updatedAtMs = now(); }
    }

    public void pause(Long sessionId, double positionSec) {
        Audio a = audio(sessionId);
        synchronized (a) { a.playing = false; a.positionSec = Math.max(0, positionSec); a.updatedAtMs = now(); }
    }

    public void seek(Long sessionId, double positionSec, Boolean playing) {
        Audio a = audio(sessionId);
        synchronized (a) {
            a.positionSec = Math.max(0, positionSec);
            if (playing != null) a.playing = playing;
            a.updatedAtMs = now();
        }
    }

    public void stop(Long sessionId) {
        Audio a = audio(sessionId);
        synchronized (a) { a.playing = false; a.positionSec = 0; a.updatedAtMs = now(); }
    }

    /** 배속 변경. 클라가 보낸 현재 위치로 기준을 다시 잡아 경과시간 보정이 어긋나지 않게 한다. */
    public void setRate(Long sessionId, double rate, double positionSec) {
        Audio a = audio(sessionId);
        synchronized (a) {
            a.rate = Math.max(MIN_RATE, Math.min(MAX_RATE, rate <= 0 ? 1.0 : rate));
            a.positionSec = Math.max(0, positionSec);
            a.updatedAtMs = now();
        }
    }

    /** AB 반복 구간 설정(각 클라가 로컬에서 loopEnd 도달 시 loopStart로 되감아 동일 구간 반복). */
    public void setLoop(Long sessionId, boolean on, double start, double end) {
        Audio a = audio(sessionId);
        synchronized (a) {
            a.loopOn = on;
            a.loopStart = Math.max(0, start);
            a.loopEnd = Math.max(0, end);
        }
    }

    /**
     * 입장/재동기화용 현재 상태 스냅샷. 재생 중이면 배속을 반영한 경과시간을 더해 "현재 위치"를 계산해 돌려준다.
     */
    public Map<String, Object> snapshot(Long sessionId) {
        Audio a = audios.get(sessionId);
        Map<String, Object> out = new LinkedHashMap<>();
        long now = now();
        out.put("serverNowMs", now);
        if (a == null) {
            out.put("playlist", new ArrayList<>());
            out.put("url", null);
            out.put("playing", false);
            out.put("positionSec", 0.0);
            out.put("rate", 1.0);
            out.put("loopOn", false);
            return out;
        }
        synchronized (a) {
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
        }
        return out;
    }

    public void clear(Long sessionId) {
        audios.remove(sessionId);
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}
