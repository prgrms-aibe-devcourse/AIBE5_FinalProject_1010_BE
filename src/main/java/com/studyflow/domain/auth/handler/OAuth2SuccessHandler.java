package com.studyflow.domain.auth.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 소셜 로그인 인증 성공 시 처리 핸들러.
 *
 * <p>기존 회원: JWT를 Redis에 저장하고 one-time code만 URL에 담아 FE로 리다이렉트.
 *   FE는 POST /api/v1/auth/oauth2/token 으로 code를 교환해 토큰을 받습니다.
 *   → accessToken·refreshToken이 브라우저 히스토리/로그에 남지 않습니다.
 *
 * <p>신규 회원: pendingSocialToken(Redis 키)을 URL에 담아 추가 정보 입력 폼으로 리다이렉트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    /** one-time code TTL: 30초 */
    private static final long CODE_TTL_SECONDS = 30L;

    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${oauth2.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 신규 회원: 추가 정보 입력 폼으로 리다이렉트
        String pendingSocialToken = (String) oAuth2User.getAttributes().get("pendingSocialToken");
        if (pendingSocialToken != null) {
            log.info("신규 소셜 유저 → 추가 정보 입력 페이지로 리다이렉트");
            getRedirectStrategy().sendRedirect(request, response,
                    frontendUrl + "/oauth2/additional-info?token=" + pendingSocialToken);
            return;
        }

        // 기존 회원: JWT 발급 후 one-time code를 Redis에 저장하고 code만 URL에 포함
        Long userId = (Long) oAuth2User.getAttributes().get("userId");
        String role  = (String) oAuth2User.getAttributes().get("role");

        String accessToken  = jwtTokenProvider.createAccessToken(userId, role);
        String refreshToken = jwtTokenProvider.createRefreshToken(userId, role);

        // refresh token → Redis (rt:{userId})
        redisTemplate.opsForValue().set(
                RedisPrefixProvider.refreshTokenKey(userId),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        // one-time code → Redis (oauth2:code:{uuid}, TTL 30초)
        String code = UUID.randomUUID().toString();
        Map<String, Object> codeData = Map.of(
                "accessToken",    accessToken,
                "refreshToken",   refreshToken,
                "accessExpiresIn",  jwtTokenProvider.getAccessTokenExpiration(),
                "refreshExpiresIn", jwtTokenProvider.getRefreshTokenExpiration()
        );
        redisTemplate.opsForValue().set(
                RedisPrefixProvider.oauth2CodeKey(code),
                objectMapper.writeValueAsString(codeData),
                CODE_TTL_SECONDS,
                TimeUnit.SECONDS
        );

        // URL에는 code만 포함 (토큰 직접 노출 없음)
        getRedirectStrategy().sendRedirect(request, response,
                frontendUrl + "/oauth2/callback?code=" + code);
    }
}
