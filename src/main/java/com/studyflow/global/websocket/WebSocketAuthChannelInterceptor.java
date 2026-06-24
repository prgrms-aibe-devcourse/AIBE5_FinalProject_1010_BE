package com.studyflow.global.websocket;

import com.studyflow.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * STOMP CONNECT 요청에서 JWT를 검증하는 인터셉터.
 *
 * REST API는 JwtAuthenticationFilter가 Authorization 헤더를 검사하지만,
 * WebSocket 메시지는 HTTP 필터만으로는 사용자 식별이 부족하다.
 * 그래서 STOMP CONNECT 프레임의 Authorization 헤더를 읽어서 Principal을 직접 넣어준다.
 */
@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        /*
         * StompHeaderAccessor.wrap(message)는 헤더를 복사한 새 accessor를 만들기 때문에
         * 거기에 setUser()를 해도 실제 채널로 흘러가는 원본 message에는 반영되지 않는다.
         * 원본 message에 연결된 mutable accessor를 가져와야 setUser()가 반영된다.
         */
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        // CONNECT: 토큰이 있으면 검증해 userId Principal, 없으면 게스트(비로그인 미리보기) Principal.
        if (StompCommand.CONNECT.equals(command)) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            if (StringUtils.hasText(authorization) && authorization.startsWith("Bearer ")) {
                Long userId = jwtTokenProvider.getUserId(authorization.substring(7));
                accessor.setUser(new StompPrincipal(userId));
            } else {
                // 비로그인 게스트 — 실시간 강의실 미리보기 시청자. 화이트보드 구독만 허용된다.
                accessor.setUser(new StompPrincipal(null));
            }
            return message;
        }

        // 게스트는 화이트보드 토픽 구독만 허용 — 채팅 등 다른 토픽 구독/발행은 차단(드롭).
        if (isGuest(accessor.getUser())) {
            if (StompCommand.SUBSCRIBE.equals(command)) {
                if (!isWhiteboardTopic(accessor.getDestination())) {
                    return null; // 비허용 구독 드롭
                }
            } else if (StompCommand.SEND.equals(command)) {
                return null; // 게스트는 발행 불가(판서/채팅 등)
            }
        }

        return message;
    }

    private boolean isGuest(java.security.Principal principal) {
        return principal instanceof StompPrincipal sp && sp.isGuest();
    }

    // /sub/classroom-sessions/{sessionId}/whiteboard 형태만 게스트에게 허용
    private boolean isWhiteboardTopic(String destination) {
        return destination != null
                && destination.startsWith("/sub/classroom-sessions/")
                && destination.endsWith("/whiteboard");
    }
}
