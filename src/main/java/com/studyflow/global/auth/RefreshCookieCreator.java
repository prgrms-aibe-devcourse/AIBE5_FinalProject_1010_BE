package com.studyflow.global.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieCreator {
    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // helper: refresh token을 HttpOnly 쿠키로 만드는 공통 로직
    public ResponseCookie createRefreshCookie(String refreshToken, long refreshExpiresInMillis) {
        boolean isLocal = "local".equalsIgnoreCase(activeProfile);
        boolean secure = !isLocal;
        String sameSite = isLocal ? "Lax" : "None";
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(refreshExpiresInMillis / 1000)
                .sameSite(sameSite)
                .build();
    }
}
