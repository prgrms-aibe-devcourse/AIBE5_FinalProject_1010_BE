package com.studyflow.domain.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 소셜 로그인 인증 실패 시 FE 로그인 페이지로 리다이렉트합니다.
 * 내부 예외 메시지는 서버 로그에만 남기고 URL에는 노출하지 않습니다.
 */
@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth2.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 내부 예외 메시지는 서버 로그에만 기록
        log.error("소셜 로그인 실패: {}", exception.getMessage());

        // URL에는 구체적인 원인 대신 일반 오류 코드만 포함
        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl + "/#/oauth2/callback?error=social_login_failed");
    }
}
