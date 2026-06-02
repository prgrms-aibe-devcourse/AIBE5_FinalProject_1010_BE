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

    // JWT 필터가 동작하되 인증 없이도 접근 허용하는 URL — 토큰이 있으면 인증 처리, 없으면 통과
    // 수업 상세: 비로그인도 조회 가능하되 로그인 시 myStatus 등 추가 정보 제공
    // /* 패턴 유지 이유: /** 로 변경하면 SecurityConfig의 POST .../enrollment-requests hasRole("STUDENT") 규칙보다
    //                    앞에서 매칭되어 학생 전용 엔드포인트가 비인증 접근을 허용하게 됨
    public String[] getOptionalAuthUrls() {
        return new String[] {
                "/api/v1/courses/*"
        };
    }

    // access token 기반 인증이 아닌 url 목록
    // JwtAuthenticationFilter에서는 제외하지 않음
    public String[] getUrlsWithoutAccessToken() {
        return new String[] {
                "/api/v1/auth/reissue"
        };
    }
}
