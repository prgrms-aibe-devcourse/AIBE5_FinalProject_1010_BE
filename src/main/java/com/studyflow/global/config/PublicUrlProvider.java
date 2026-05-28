package com.studyflow.global.config;

import org.springframework.stereotype.Component;

//애플리케이션에서 공개(permitAll) URL 목록을 중앙에서 관리합니다.
@Component
public class PublicUrlProvider {
    public String[] getPublicUrls() {
        return new String[] {
                "/api/v1/auth/signup",
                "/api/v1/auth/login"
        };
    }
}

