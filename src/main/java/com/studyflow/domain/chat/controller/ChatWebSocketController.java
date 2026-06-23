package com.studyflow.domain.chat.controller;

import com.studyflow.domain.chat.dto.request.ChatMessageSendRequest;
import com.studyflow.domain.chat.dto.request.ChatCallSignalRequest;
import com.studyflow.domain.chat.dto.request.ChatReadRequest;
import com.studyflow.domain.chat.dto.response.ChatCallSignalResponse;
import com.studyflow.domain.chat.dto.response.ChatErrorResponse;
import com.studyflow.domain.chat.dto.response.ChatMessageResponse;
import com.studyflow.domain.chat.dto.response.ChatReadResponse;
import com.studyflow.domain.chat.service.ChatCallSignalService;
import com.studyflow.domain.chat.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final ChatCallSignalService chatCallSignalService;
    private final com.studyflow.global.realtime.RealtimeBroadcaster broadcaster;

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

        broadcaster.send(
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

        broadcaster.send(
                "/sub/chat-rooms/" + roomId + "/read",
                response
        );
    }

    /**
     * 보이스톡 WebRTC 신호 중계.
     *
     * 클라이언트 발행 경로:
     * /pub/chat-rooms/{roomId}/calls
     *
     * 클라이언트 구독 경로:
     * /sub/chat-rooms/{roomId}/calls
     */
    @MessageMapping("/chat-rooms/{roomId}/calls")
    public void relayCallSignal(
            @DestinationVariable Long roomId,
            ChatCallSignalRequest request,
            Principal principal
    ) {
        Long currentUserId = extractUserId(principal);

        ChatCallSignalResponse response = chatCallSignalService.createSignal(
                roomId,
                currentUserId,
                request
        );

        broadcaster.send(
                "/sub/chat-rooms/" + roomId + "/calls",
                response
        );
    }

    /**
     * WebSocket 메시지 처리 중 발생한 예외 처리.
     *
     * @MessageMapping 메서드에서 예외가 나면 메시지가 조용히 사라지고
     * 클라이언트는 왜 실패했는지 알 수 없다.
     * 여기서 예외를 잡아 요청한 사용자 본인에게만 에러를 전달한다.
     *
     * @SendToUser는 user destination prefix를 붙여 개인 큐로 보내므로,
     * 클라이언트는 "/user/sub/errors"를 구독하면 자신의 에러만 받는다.
     */
    @MessageExceptionHandler
    @SendToUser("/sub/errors")
    public ChatErrorResponse handleException(
            Throwable exception,
            @Header(name = "simpDestination", required = false) String destination
    ) {
        log.warn("WebSocket 채팅 처리 실패: destination={}, message={}",
                destination, exception.getMessage());

        return new ChatErrorResponse(
                extractRoomId(destination),
                exception.getMessage()
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

    /**
     * "/pub/chat-rooms/{roomId}/messages" 형태의 목적지에서 roomId를 추출한다.
     *
     * 목적지를 알 수 없거나 형식이 맞지 않으면 null을 반환한다.
     */
    private Long extractRoomId(String destination) {
        if (destination == null) {
            return null;
        }

        String marker = "/chat-rooms/";
        int start = destination.indexOf(marker);
        if (start < 0) {
            return null;
        }

        start += marker.length();
        int end = destination.indexOf('/', start);
        String roomIdPart = end < 0
                ? destination.substring(start)
                : destination.substring(start, end);

        try {
            return Long.valueOf(roomIdPart);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
