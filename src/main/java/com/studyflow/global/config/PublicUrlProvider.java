package com.studyflow.global.config;

import org.springframework.stereotype.Component;

//애플리케이션에서 공개(permitAll) URL 목록을 중앙에서 관리합니다.
@Component
public class PublicUrlProvider {
    // 인증 정보 저장 및 인증 필터링 과정 전체를 생략하는 url 목록
    public String[] getPublicUrls() {
        return new String[] {
                "/api/v1/auth/email/code/send",
                "/api/v1/auth/email/verify",
                "/api/v1/auth/signup",
                "/api/v1/auth/login",
                "/api/v1/auth/social-pending",  // 소셜 로그인 폼 pre-fill 데이터 조회 (POST)
                "/api/v1/auth/social-signup",   // 소셜 로그인 추가 정보 입력 후 가입 완료
                "/api/v1/auth/oauth2/token",    // 소셜 로그인 one-time code → 토큰 교환

                // 과목 목록 — 수업 등록/검색 폼에서 비로그인 사용자도 조회 가능
                "/api/v1/subjects",

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

    // GET 요청에 한하여, 유효한 access token이 있으면 읽어들이고, 없으면 스킵
    // SecurityConfig에서는 permitAll(), jwtAuthenticationFilter는 건너뛰지 않음
    // GET 요청이 아니면 일반 access token 검증 로직을 거침
    // GET 요청임에도 인증이 필요한 경우, controller에서 인증 직접 확인 필요
    public String[] getOptionalAuthUrls() {
        return new String[] {
                // 수업 검색(목록) · 수업 상세 GET — 비로그인 허용
                // /api/v1/courses는 POST(TEACHER 전용)가 있어 PublicUrls에서 제외하고 GET만 명시적 허용
                "/api/v1/courses",
                "/api/v1/courses/*",
                // 선생님 목록 및 상세 — 비로그인 사용자도 조회 가능
                "/api/v1/teachers",
                "/api/v1/teachers/*",
                // QnA 질문 목록·상세 GET — 비로그인 허용. (POST/PATCH/DELETE는 SecurityConfig 역할/인증 규칙이 보호)
                // 로그인 상태로 GET 시에는 토큰을 읽어 좋아요 여부(liked) 계산에 사용한다.
                "/api/v1/qna/questions",
                "/api/v1/qna/questions/*"
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
