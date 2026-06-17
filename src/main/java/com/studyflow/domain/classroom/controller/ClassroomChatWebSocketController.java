package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.chat.dto.response.ChatErrorResponse;
import com.studyflow.domain.classroom.dto.request.ClassroomChatLikeRequest;
import com.studyflow.domain.classroom.dto.request.ClassroomChatSendRequest;
import com.studyflow.domain.classroom.dto.response.ClassroomChatLikeResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomChatResponse;
import com.studyflow.domain.classroom.service.ClassroomChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

/**
 * 강의실 채팅 WebSocket 컨트롤러 (apidetail.md 23-2).
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/chats — 구독: /sub/classroom-sessions/{sessionId}/chats.
 * 인증은 STOMP CONNECT 시 WebSocketAuthChannelInterceptor가 Principal에 userId를 넣어둔다.
 * 멤버십·세션 상태 검증은 서비스에서 수행하며, 실패 시 본인에게만 /user/sub/errors로 전달한다.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomChatWebSocketController {

    private final ClassroomChatService classroomChatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/classroom-sessions/{sessionId}/chats")
    public void sendMessage(
            @DestinationVariable Long sessionId,
            ClassroomChatSendRequest request,
            Principal principal
    ) {
        Long userId = extractUserId(principal);

        ClassroomChatResponse response = classroomChatService.sendMessage(
                sessionId, userId, request);

        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/chats",
                response
        );
    }

    /** 채팅 메시지 좋아요 토글 — 변경된 좋아요 수를 전원에게 브로드캐스트. */
    @MessageMapping("/classroom-sessions/{sessionId}/chat-likes")
    public void toggleLike(
            @DestinationVariable Long sessionId,
            ClassroomChatLikeRequest request,
            Principal principal
    ) {
        Long userId = extractUserId(principal);
        ClassroomChatLikeResponse response = classroomChatService.toggleLike(sessionId, userId, request.chatId());
        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/chat-likes",
                response
        );
    }

    /**
     * 처리 중 예외를 요청자 본인에게만 전달한다(1:1 채팅과 동일하게 /user/sub/errors 사용).
     * roomId 자리에 sessionId를 담는다.
     */
    @MessageExceptionHandler
    @SendToUser("/sub/errors")
    public ChatErrorResponse handleException(
            Throwable exception,
            @Header(name = "simpDestination", required = false) String destination
    ) {
        log.warn("강의실 채팅 처리 실패: destination={}, message={}", destination, exception.getMessage());
        return new ChatErrorResponse(extractSessionId(destination), exception.getMessage());
    }

    private Long extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("WebSocket 인증 정보가 없습니다.");
        }
        return Long.valueOf(principal.getName());
    }

    /** "/pub/classroom-sessions/{sessionId}/chats" 형태에서 sessionId 추출(실패 시 null). */
    private Long extractSessionId(String destination) {
        if (destination == null) {
            return null;
        }
        String marker = "/classroom-sessions/";
        int start = destination.indexOf(marker);
        if (start < 0) {
            return null;
        }
        start += marker.length();
        int end = destination.indexOf('/', start);
        String part = end < 0 ? destination.substring(start) : destination.substring(start, end);
        try {
            return Long.valueOf(part);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
