package com.studyflow.global.config;

import com.studyflow.global.websocket.WebSocketAuthChannelInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;

    /**
     * WebSocket 연결 endpoint.
     *
     * 클라이언트는 /ws-stomp로 연결한다.
     * SockJS를 사용하므로 브라우저 호환성이 조금 더 좋다.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * /pub: 클라이언트가 서버로 메시지를 보내는 prefix.
     * /sub: 클라이언트가 서버 메시지를 구독하는 prefix.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/sub");
        registry.setApplicationDestinationPrefixes("/pub");
    }

    /**
     * STOMP CONNECT 단계에서 JWT를 검증한다.
     *
     * CONNECT 헤더 예:
     * Authorization: Bearer {accessToken}
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }

    /**
     * 메시지 크기 제한 — 기본 64KB는 화이트보드 op에 작다(전체선택 후 일괄 이동 시 펜 획들의 점 배열이 한 메시지에 모임).
     * 단, 이미지는 base64가 아니라 URL로만 동기화되고 입장/재동기화(전체 보드)는 WS가 아닌 REST라 이 제한과 무관하므로,
     * 현실 최악 케이스(수천 획 일괄 이동 ≈ 수 MB)에 여유를 둔 16MB면 충분하다.
     * (이 값은 "한 메시지를 이만큼까지 버퍼링 허용"하는 상한이므로 과도하게 키우면 메모리/DoS 노출만 커진다.)
     */
    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(16 * 1024 * 1024);      // 16MB (인바운드 한 메시지)
        registration.setSendBufferSizeLimit(32 * 1024 * 1024);   // 32MB (아웃바운드 버퍼)
        registration.setSendTimeLimit(60 * 1000);                // 60s
    }
}
