package com.studyflow.domain.auth.handler;

import com.studyflow.global.auth.JwtTokenProvider;
import com.studyflow.global.redis.RedisPrefixProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 소셜 로그인 인증 성공 시 처리 핸들러.
 *
 * <p>기존 회원: JWT 발급 후 프론트엔드 /oauth2/callback 으로 리다이렉트
 * <p>신규 회원: gender/birthDate 등 필수 정보가 부족할 수 있으므로
 *              Redis에 임시 저장된 pendingSocialToken을 이용해
 *              추가 정보 입력 폼(/oauth2/additional-info)으로 리다이렉트
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;

    @Value("${cors.allowed-origins}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 신규 회원: pendingSocialToken이 있으면 추가 정보 입력 페이지로 리다이렉트
        String pendingSocialToken = (String) oAuth2User.getAttributes().get("pendingSocialToken");
        if (pendingSocialToken != null) {
            String redirectUrl = frontendUrl + "/oauth2/additional-info?token=" + pendingSocialToken;
            log.info("신규 소셜 유저 감지 → 추가 정보 입력 페이지로 리다이렉트");
            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        // 기존 회원: JWT 발급 후 콜백 페이지로 리다이렉트
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        String role  = (String) oAuth2User.getAttributes().get("role");

        String accessToken  = jwtTokenProvider.createAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, role);

        redisTemplate.opsForValue().set(
                RedisPrefixProvider.refreshTokenKey(userId),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        String redirectUrl = frontendUrl + "/oauth2/callback"
                + "?accessToken=" + accessToken
                + "&refreshToken=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
