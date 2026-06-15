package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.WhiteboardSnapshotStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 강의실 화이트보드 실시간 동기화 WebSocket 컨트롤러 (이슈 #131).
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/whiteboard — 구독: /sub/classroom-sessions/{sessionId}/whiteboard.
 * 화이트보드 변경(op)을 같은 세션 참가자에게 그대로 중계한다(서버는 op 의미를 해석하지 않는 relay).
 * 메시지 payload는 {type, ...op}이며, 서버가 senderId(보낸 사용자)를 주입해 클라이언트가 자기 메시지를 무시할 수 있게 한다.</p>
 *
 * <p>type == "snapshot"인 경우 브로드캐스트하지 않고 세션별 현재 보드를 메모리에 저장한다(늦게 입장한 사람용).
 * 인증은 STOMP CONNECT 시 WebSocketAuthChannelInterceptor가 Principal에 userId를 넣어둔다.
 * op는 고빈도라 메시지마다 DB 멤버십 검증은 하지 않고 CONNECT 인증으로 게이팅한다(권한 세분화는 후속).</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomWhiteboardWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WhiteboardSnapshotStore snapshotStore;

    @MessageMapping("/classroom-sessions/{sessionId}/whiteboard")
    public void relay(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        if (principal != null) {
            message.put("senderId", Long.valueOf(principal.getName()));
        }

        // 전체 보드 스냅샷은 저장만 하고 브로드캐스트하지 않는다(늦게 입장자가 REST로 받음).
        if ("snapshot".equals(message.get("type"))) {
            snapshotStore.put(sessionId, message.get("board"));
            return;
        }

        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/whiteboard",
                message
        );
    }
}
