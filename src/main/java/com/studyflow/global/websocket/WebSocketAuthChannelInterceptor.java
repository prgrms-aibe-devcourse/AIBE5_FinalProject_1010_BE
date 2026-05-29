package com.studyflow.global.websocket;

import com.studyflow.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
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
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader("Authorization");
            String token = resolveToken(authorization);

            Long userId = jwtTokenProvider.getUserId(token);
            accessor.setUser(new StompPrincipal(userId));
        }

        return message;
    }

    private String resolveToken(String authorization) {
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("WebSocket Authorization 헤더가 필요합니다.");
        }

        return authorization.substring(7);
    }
}
