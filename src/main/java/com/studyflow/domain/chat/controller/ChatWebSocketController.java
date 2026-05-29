package com.studyflow.domain.chat.controller;

import com.studyflow.domain.chat.dto.request.ChatMessageSendRequest;
import com.studyflow.domain.chat.dto.request.ChatReadRequest;
import com.studyflow.domain.chat.dto.response.ChatMessageResponse;
import com.studyflow.domain.chat.dto.response.ChatReadResponse;
import com.studyflow.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * WebSocket 메시지 전송.
     *
     * 클라이언트 발행 경로:
     * /pub/chat-rooms/{roomId}/messages
     *
     * 클라이언트 구독 경로:
     * /sub/chat-rooms/{roomId}/messages
     *
     * 처리 흐름:
     * 1. STOMP CONNECT에서 인증된 Principal에서 userId를 꺼낸다.
     * 2. 메시지를 DB에 저장한다.
     * 3. 저장된 메시지를 같은 채팅방 구독자에게 브로드캐스트한다.
     */
    @MessageMapping("/chat-rooms/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable Long roomId,
            ChatMessageSendRequest request,
            Principal principal
    ) {
        Long currentUserId = extractUserId(principal);

        ChatMessageResponse response = chatService.sendMessage(
                currentUserId,
                roomId,
                request
        );

        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + roomId + "/messages",
                response
        );
    }

    /**
     * WebSocket 읽음 처리.
     *
     * 클라이언트 발행 경로:
     * /pub/chat-rooms/{roomId}/read
     *
     * 클라이언트 구독 경로:
     * /sub/chat-rooms/{roomId}/read
     */
    @MessageMapping("/chat-rooms/{roomId}/read")
    public void readMessage(
            @DestinationVariable Long roomId,
            ChatReadRequest request,
            Principal principal
    ) {
        Long currentUserId = extractUserId(principal);

        ChatReadResponse response = chatService.readUpTo(
                currentUserId,
                roomId,
                request.getLastReadMessageId()
        );

        messagingTemplate.convertAndSend(
                "/sub/chat-rooms/" + roomId + "/read",
                response
        );
    }

    /**
     * WebSocketAuthChannelInterceptor에서 userId를 Principal.name에 넣어둔다.
     */
    private Long extractUserId(Principal principal) {
        if (principal == null) {
            throw new IllegalArgumentException("WebSocket 인증 정보가 없습니다.");
        }

        return Long.valueOf(principal.getName());
    }
}
