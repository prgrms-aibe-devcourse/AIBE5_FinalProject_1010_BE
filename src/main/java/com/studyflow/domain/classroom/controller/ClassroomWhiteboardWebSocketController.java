package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.WhiteboardStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 강의실 화이트보드 실시간 동기화 WebSocket 컨트롤러 — 서버 권위(authoritative) 방식 (이슈 #131).
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/whiteboard — 구독: /sub/classroom-sessions/{sessionId}/whiteboard.
 * 서버가 단일 진실원천이다. 클라이언트가 보낸 변경 의도(ops)를 서버의 권위 상태에 반영하고,
 * 단조 증가하는 순번(seq)을 붙여 같은 세션 전원에게(보낸 사람 포함) 재방송한다.
 * 모든 클라이언트가 동일한 seq 순서로 동일한 ops를 적용 → 화면이 분기하지 않는다.</p>
 *
 * <p>메시지 종류:
 * <ul>
 *   <li>type == "ops": {ops:[...]} — 상태에 반영 후 seq를 붙여 재방송. 클라는 seq 순서대로 적용하고
 *       구멍이 나면 REST로 전체 상태를 다시 받아 자가 치유한다.</li>
 *   <li>type == "live": 그리는 중 미리보기 — 상태 변경/순번 없이 그대로 중계(휘발성).</li>
 * </ul>
 * 인증은 STOMP CONNECT 시 WebSocketAuthChannelInterceptor가 Principal에 userId를 넣어둔다.
 * op는 고빈도라 메시지마다 DB 멤버십 검증은 하지 않고 CONNECT 인증으로 게이팅한다(권한 세분화는 후속).</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomWhiteboardWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WhiteboardStateStore stateStore;

    @MessageMapping("/classroom-sessions/{sessionId}/whiteboard")
    public void relay(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        if (principal != null) {
            message.put("senderId", Long.valueOf(principal.getName()));
        }

        String type = String.valueOf(message.get("type"));

        // 그리는 중 미리보기: 상태를 바꾸지 않는 휘발성 메시지. 순번 없이 그대로 중계.
        if ("live".equals(type)) {
            broadcast(sessionId, message);
            return;
        }

        // 확정 변경: 서버 권위 상태에 반영하고 순번(seq)을 붙여 전원에게 재방송.
        if ("ops".equals(type)) {
            long seq = stateStore.apply(sessionId, castOps(message.get("ops")));
            message.put("seq", seq);
            broadcast(sessionId, message);
            return;
        }
        // 그 외(과거 snapshot 등) 타입은 무시 — 서버가 이미 권위 상태를 들고 있다.
    }

    private void broadcast(Long sessionId, Map<String, Object> message) {
        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/whiteboard",
                message
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castOps(Object ops) {
        return (ops instanceof List) ? (List<Map<String, Object>>) ops : null;
    }
}
