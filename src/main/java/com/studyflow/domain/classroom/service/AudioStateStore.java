package com.studyflow.domain.classroom.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 강의실 듣기 자료(오디오)의 "권위 있는 현재 재생 상태"를 세션별로 메모리에 보관한다 (이슈 #182).
 *
 * <p>화이트보드({@link WhiteboardStateStore})와 같은 철학: 서버가 단일 진실원천이다. 선생님(호스트)이
 * load/play/pause/seek/stop을 보내면 이 상태에 반영한 뒤 같은 세션 전원에게 재방송한다.
 * 늦게 들어온 참가자는 REST 스냅샷으로 현재 트랙/재생여부/위치를 받아 동기화한다.</p>
 *
 * <p>오디오 "제어"는 호스트(수업 진행 선생님)만 가능하다. 호스트 식별자는 입장 시
 * {@code setHost}로 기록하고, WebSocket 컨트롤러가 {@code isHost}로 게이팅한다(고빈도 아님).
 * 단일 인스턴스 in-memory — 다중 인스턴스 확장 시 Redis 등으로 대체.</p>
 */
@Component
public class AudioStateStore {

    /** 한 세션의 오디오 재생 컨텍스트. */
    static final class Audio {
        Long hostUserId;        // 오디오 제어 권한자(수업 진행 선생님)
        String url;             // 현재 트랙 URL (null = 선택된 트랙 없음)
        String fileName;
        Long fileId;
        boolean playing;
        double positionSec;     // updatedAtMs 기준 시점의 재생 위치(초)
        long updatedAtMs;       // 위 상태가 설정된 서버 epoch ms (재생 중일 때 경과시간 보정용)
    }

    private final Map<Long, Audio> audios = new ConcurrentHashMap<>();

    private Audio audio(Long sessionId) {
        return audios.computeIfAbsent(sessionId, k -> new Audio());
    }

    /** 호스트(제어 권한자) 등록 — 입장 시 호출. */
    public void setHost(Long sessionId, Long userId) {
        Audio a = audio(sessionId);
        synchronized (a) { a.hostUserId = userId; }
    }

    /** 해당 사용자가 이 세션의 오디오 제어 권한자(호스트)인지. */
    public boolean isHost(Long sessionId, Long userId) {
        Audio a = audios.get(sessionId);
        return a != null && userId != null && userId.equals(a.hostUserId);
    }

    /** 새 트랙 선택(재생 정지·위치 0으로 초기화). */
    public void load(Long sessionId, String url, String fileName, Long fileId) {
        Audio a = audio(sessionId);
        synchronized (a) {
            a.url = url;
            a.fileName = fileName;
            a.fileId = fileId;
            a.playing = false;
            a.positionSec = 0;
            a.updatedAtMs = now();
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

    /** 정지 — 재생 멈추고 위치를 처음(0)으로. 트랙은 유지. */
    public void stop(Long sessionId) {
        Audio a = audio(sessionId);
        synchronized (a) { a.playing = false; a.positionSec = 0; a.updatedAtMs = now(); }
    }

    /**
     * 입장/재동기화용 현재 상태 스냅샷.
     * 재생 중이면 경과 시간을 더한 "현재 위치"를 서버에서 계산해 돌려준다(늦은 입장자가 바로 같은 위치로 맞추도록).
     */
    public Map<String, Object> snapshot(Long sessionId) {
        Audio a = audios.get(sessionId);
        Map<String, Object> out = new LinkedHashMap<>();
        long now = now();
        out.put("serverNowMs", now);
        if (a == null || a.url == null) {
            out.put("url", null);
            out.put("playing", false);
            out.put("positionSec", 0.0);
            return out;
        }
        synchronized (a) {
            double effective = a.playing ? a.positionSec + (now - a.updatedAtMs) / 1000.0 : a.positionSec;
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
