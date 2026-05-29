package com.studyflow.global.config;

import org.springframework.stereotype.Component;

//애플리케이션에서 공개(permitAll) URL 목록을 중앙에서 관리합니다.
@Component
public class PublicUrlProvider {
    public String[] getPublicUrls() {
        return new String[] {
                "/api/v1/auth/signup",
                "/api/v1/auth/login",

                // WebSocket handshake와 SockJS 부가 요청은 HTTP 필터에서 막지 않는다.
                // 실제 채팅 메시지 인증은 WebSocketAuthChannelInterceptor가 STOMP CONNECT에서 처리한다.
                "/ws-stomp/**"
        };
    }
}
