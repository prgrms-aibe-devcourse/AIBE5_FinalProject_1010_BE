package com.studyflow.global.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

import java.util.Arrays;
import java.util.Base64;

/**
 * OAuth2 인가 요청(state, redirect_uri 등)을 HTTP 세션 대신 쿠키에 저장합니다.
 *
 * <p>STATELESS 환경에서 세션을 사용하면 다중 서버 배포 시 세션 불일치가 발생합니다.
 * 쿠키 방식은 서버를 여러 대로 늘려도 상태가 클라이언트에 있으므로 안전합니다.
 *
 * <p>직렬화: {@link SerializationUtils}로 Java 직렬화 후 Base64URL 인코딩.
 * {@link OAuth2AuthorizationRequest}는 {@link java.io.Serializable}을 구현합니다.
 */
@Slf4j
@Component
public class CookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String COOKIE_NAME    = "oauth2_auth_req";
    private static final int    COOKIE_MAX_AGE = 180; // 3분

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return readCookie(request);
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            deleteCookie(request, response);
            return;
        }
        String value = serialize(authorizationRequest);
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(COOKIE_MAX_AGE);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        OAuth2AuthorizationRequest authRequest = readCookie(request);
        if (authRequest != null) {
            deleteCookie(request, response);
        }
        return authRequest;
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private OAuth2AuthorizationRequest readCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .findFirst()
                .map(c -> deserialize(c.getValue()))
                .orElse(null);
    }

    private void deleteCookie(HttpServletRequest request, HttpServletResponse response) {
        if (request.getCookies() == null) return;
        Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .forEach(c -> {
                    Cookie expired = new Cookie(COOKIE_NAME, "");
                    expired.setPath("/");
                    expired.setMaxAge(0);
                    response.addCookie(expired);
                });
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        return Base64.getUrlEncoder().encodeToString(
                SerializationUtils.serialize(authorizationRequest));
    }

    private OAuth2AuthorizationRequest deserialize(String value) {
        try {
            byte[] bytes = Base64.getUrlDecoder().decode(value);
            return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
        } catch (Exception e) {
            log.warn("OAuth2 인가 요청 쿠키 역직렬화 실패: {}", e.getMessage());
            return null;
        }
    }
}
