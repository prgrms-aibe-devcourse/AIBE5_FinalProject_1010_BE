package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.AudioStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 강의실 듣기 자료(오디오) 재생 동기화 WebSocket 컨트롤러 (이슈 #182).
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/audio — 구독: /sub/classroom-sessions/{sessionId}/audio.
 * 화이트보드와 같은 "서버 권위" 방식: 선생님(호스트)이 보낸 제어를 서버 상태({@link AudioStateStore})에
 * 반영한 뒤 같은 세션 전원에게(보낸 사람 포함) 재방송한다. 늦게 들어온 참가자는 REST 스냅샷으로 맞춘다.</p>
 *
 * <p>메시지 종류(type):
 * <ul>
 *   <li>add         {url, fileName, fileId} — 재생목록에 트랙 추가</li>
 *   <li>select      {fileId}                — 재생목록에서 트랙 선택(정지·0초·반복해제)</li>
 *   <li>removeTrack {fileId}                — 재생목록에서 트랙 삭제</li>
 *   <li>play        {positionSec}           — 재생</li>
 *   <li>pause       {positionSec}           — 일시정지</li>
 *   <li>seek        {positionSec, playing?} — 위치 이동(정밀 탐색)</li>
 *   <li>stop                                — 정지(0초로 되감기)</li>
 *   <li>rate        {rate, positionSec}     — 재생 배속(0.2~3x)</li>
 *   <li>loop        {loopOn, loopStart, loopEnd} — AB 반복 구간</li>
 * </ul>
 * 오디오 "제어"는 호스트(수업 진행 선생님)만 가능하다 — 권한 없는 참가자(학생)의 메시지는 조용히 무시한다.
 * 인증은 STOMP CONNECT 시 WebSocketAuthChannelInterceptor가 Principal에 userId를 넣어둔다.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomAudioWebSocketController {

    private final com.studyflow.global.realtime.RealtimeBroadcaster broadcaster;
    private final AudioStateStore audioStateStore;

    @MessageMapping("/classroom-sessions/{sessionId}/audio")
    public void relay(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        if (principal == null) {
            return;
        }
        Long senderId = Long.valueOf(principal.getName());

        // 오디오 제어는 호스트(선생님)만 — 권한 없는 참가자의 메시지는 무시.
        if (!audioStateStore.isHost(sessionId, senderId)) {
            return;
        }
        message.put("senderId", senderId);

        String type = String.valueOf(message.get("type"));
        switch (type) {
            case "add" -> audioStateStore.add(sessionId,
                    str(message.get("url")), str(message.get("fileName")), longVal(message.get("fileId")));
            case "select" -> { if (!audioStateStore.select(sessionId, longVal(message.get("fileId")))) return; } // 목록에 없으면 상태 미변경 → 재방송 안 함
            case "removeTrack" -> audioStateStore.removeTrack(sessionId, longVal(message.get("fileId")));
            case "play" -> audioStateStore.play(sessionId, dbl(message.get("positionSec")));
            case "pause" -> audioStateStore.pause(sessionId, dbl(message.get("positionSec")));
            case "seek" -> audioStateStore.seek(sessionId, dbl(message.get("positionSec")), boolVal(message.get("playing")));
            case "stop" -> audioStateStore.stop(sessionId);
            case "rate" -> audioStateStore.setRate(sessionId, dbl(message.get("rate")), dbl(message.get("positionSec")));
            case "loop" -> audioStateStore.setLoop(sessionId,
                    Boolean.TRUE.equals(boolVal(message.get("loopOn"))), dbl(message.get("loopStart")), dbl(message.get("loopEnd")));
            default -> { return; } // 알 수 없는 타입은 무시
        }

        // 클라이언트가 위치/지연을 보정할 수 있도록 서버 시각을 실어 재방송.
        message.put("serverNowMs", System.currentTimeMillis());
        broadcaster.send(
                "/sub/classroom-sessions/" + sessionId + "/audio",
                message
        );
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static double dbl(Object o) {
        double v;
        if (o instanceof Number n) {
            v = n.doubleValue();
        } else {
            try {
                v = o == null ? 0 : Double.parseDouble(String.valueOf(o));
            } catch (NumberFormatException e) {
                v = 0;
            }
        }
        // NaN/Infinity가 상태에 들어가면 전 참가자 재생 위치가 깨지므로 차단(리뷰 반영).
        return Double.isFinite(v) ? v : 0;
    }

    private static Long longVal(Object o) {
        if (o instanceof Number n) return n.longValue();
        try {
            return o == null ? null : Long.valueOf(String.valueOf(o));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean boolVal(Object o) {
        if (o instanceof Boolean b) return b;
        if (o == null) return null;
        return Boolean.valueOf(String.valueOf(o));
    }
}
