package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;

/**
 * 강의실 리액션(손흔들기/좋아요) WebSocket 컨트롤러.
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/reactions — 구독: /sub/classroom-sessions/{sessionId}/reactions.
 * 화이트보드 live처럼 "휘발성" 메시지라 저장하지 않고 그대로 중계한다(보낸 사람·이름만 주입).
 * payload 예: {type:"like"|"hand"} → 전원에게 {type, senderId, senderName} 브로드캐스트.</p>
 */
@Controller
@RequiredArgsConstructor
public class ClassroomReactionWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    @MessageMapping("/classroom-sessions/{sessionId}/reactions")
    public void relay(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        if (principal == null) return;
        Long userId = Long.valueOf(principal.getName());
        message.put("senderId", userId);
        userRepository.findById(userId).ifPresent(u -> message.put("senderName", u.getName()));
        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/reactions",
                message
        );
    }
}
