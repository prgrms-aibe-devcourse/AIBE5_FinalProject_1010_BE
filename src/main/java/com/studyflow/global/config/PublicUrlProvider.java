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
                // Swagger UI 접근 허용
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/swagger-ui.html"
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

