package com.studyflow.global.config;

import org.springframework.stereotype.Component;

//애플리케이션에서 공개(permitAll) URL 목록을 중앙에서 관리합니다.
@Component
public class PublicUrlProvider {
    // 인증/인가를 거치지 않는 url 목록
    public String[] getPublicUrls() {
        return new String[] {
                "/api/v1/auth/signup",
                "/api/v1/auth/login",

                // 수업 검색 — 비로그인 사용자도 수업 목록 조회 가능
                "/api/v1/courses",

                // 선생님 목록 및 상세 — 비로그인 사용자도 조회 가능
                "/api/v1/teachers",
                "/api/v1/teachers/**",

                // WebSocket handshake와 SockJS 부가 요청은 HTTP 필터에서 막지 않는다.
                // 실제 채팅 메시지 인증은 WebSocketAuthChannelInterceptor가 STOMP CONNECT에서 처리한다.
                "/ws-stomp/**",

                // 업로드된 채팅 이미지 정적 제공 경로. 브라우저 <img> 가 토큰 없이 GET 하므로 허용.
                // 운영에서는 S3 + 서명 URL 등으로 보호하는 것을 권장.
                "/uploads/**",

                // Swagger UI 접근 허용
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
        };
    }

    // SecurityConfig에서 직접 HttpMethod.GET으로 제한하므로 이 메서드는 현재 미사용
    // (이전에는 GET/POST/PUT/DELETE 모두 permitAll돼서 TEACHER 규칙이 가려지는 문제가 있었음)
    public String[] getOptionalAuthUrls() {
        return new String[] {};
    }

    // access token 기반 인증이 아닌 url 목록
    // JwtAuthenticationFilter에서는 제외하지 않음
    public String[] getUrlsWithoutAccessToken() {
        return new String[] {
                "/api/v1/auth/reissue"
        };
    }
}
