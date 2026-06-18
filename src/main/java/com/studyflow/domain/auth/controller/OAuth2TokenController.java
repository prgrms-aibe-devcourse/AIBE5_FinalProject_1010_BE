package com.studyflow.domain.auth.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.auth.dto.OAuth2TokenRequest;
import com.studyflow.domain.auth.exception.SignupRequestException;
import com.studyflow.domain.auth.service.LoginHistoryService;
import com.studyflow.global.auth.RefreshCookieCreator;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.global.redis.RedisPrefixProvider;
import com.studyflow.global.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 소셜 로그인 성공 후 one-time code를 실제 토큰으로 교환하는 엔드포인트.
 * POST /api/v1/auth/oauth2/token
 *
 * <p>흐름:
 * OAuth2SuccessHandler → Redis에 code 저장 → FE로 code만 리다이렉트
 * → FE가 이 엔드포인트 호출 → Redis에서 code 소진(삭제) → 토큰 반환
 *
 * <p>인증 불필요 (PublicUrlProvider에 등록)
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class OAuth2TokenController {

    private final StringRedisTemplate redisTemplate;
    private final RefreshCookieCreator refreshCookieCreator;
    private final ObjectMapper objectMapper;
    private final LoginHistoryService loginHistoryService;

    @PostMapping("/oauth2/token")
    public ResponseEntity<?> exchangeCode(@Valid @RequestBody OAuth2TokenRequest request,
                                          HttpServletRequest httpRequest) {
        String key = RedisPrefixProvider.oauth2CodeKey(request.getCode());

        // getAndDelete: 조회와 동시에 삭제 (one-time 보장)
        String json = redisTemplate.opsForValue().getAndDelete(key);
        if (json == null) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR,
                    "유효하지 않거나 만료된 코드입니다. 소셜 로그인을 다시 시도해 주세요.");
        }

        Map<String, Object> data;
        try {
            data = objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("OAuth2 code 데이터 파싱 오류: {}", e.getMessage());
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "코드 처리 중 오류가 발생했습니다.");
        }

        // 재배포 직후 이전 형식의 코드가 Redis에 남아 있을 수 있으므로 null 방어
        Number userIdNum = (Number) data.get("userId");
        if (userIdNum == null) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR,
                    "유효하지 않거나 만료된 코드입니다. 소셜 로그인을 다시 시도해 주세요.");
        }
        Long   userId         = userIdNum.longValue();
        String name           = (String) data.get("name");
        String role           = (String) data.get("role");
        String accessToken    = (String) data.get("accessToken");
        String refreshToken   = (String) data.get("refreshToken");
        long refreshExpiresIn = ((Number) data.get("refreshExpiresIn")).longValue();
        long accessExpiresIn  = ((Number) data.get("accessExpiresIn")).longValue();

        loginHistoryService.record(userId, UserAgentParser.extractClientIp(httpRequest),
                httpRequest.getHeader("User-Agent"));

        ResponseCookie refreshCookie = refreshCookieCreator.createRefreshCookie(refreshToken, refreshExpiresIn);

        Map<String, Object> body = new HashMap<>();
        body.put("userId",          userId);
        body.put("name",            name);
        body.put("role",            role);
        body.put("accessToken",     accessToken);
        body.put("accessExpiresIn", accessExpiresIn);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }
}
