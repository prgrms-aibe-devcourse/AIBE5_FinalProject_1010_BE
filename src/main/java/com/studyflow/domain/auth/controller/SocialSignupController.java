package com.studyflow.domain.auth.controller;

import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.SocialPendingInfoResponse;
import com.studyflow.domain.auth.dto.SocialSignupRequest;
import com.studyflow.domain.auth.service.SocialSignupService;
import com.studyflow.global.auth.RefreshCookieCreator;
import com.studyflow.global.util.UserAgentParser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 소셜 로그인 추가 정보 입력 관련 엔드포인트.
 * - POST /api/v1/auth/social-pending  : 폼 pre-fill용 소셜 데이터 조회 (token을 body로 수신 — PII 로그 노출 방지)
 * - POST /api/v1/auth/social-signup   : 추가 정보 입력 후 가입 완료
 *
 * <p>두 엔드포인트 모두 인증 불필요 (PublicUrlProvider에 등록)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class SocialSignupController {

    private final SocialSignupService socialSignupService;
    private final RefreshCookieCreator refreshCookieCreator;

    /**
     * 소셜 제공자로부터 받은 사전 입력 가능한 데이터를 반환합니다.
     * token을 쿼리 파라미터 대신 POST body로 수신해 서버 접근 로그 노출을 방지합니다.
     */
    @PostMapping("/social-pending")
    public ResponseEntity<SocialPendingInfoResponse> getSocialPendingInfo(
            @Valid @RequestBody SocialPendingRequest request) {
        return ResponseEntity.ok(socialSignupService.getPendingInfo(request.getToken()));
    }

    @PostMapping("/social-signup")
    public ResponseEntity<?> socialSignup(@Valid @RequestBody SocialSignupRequest request,
                                          HttpServletRequest httpRequest) {
        String ipAddress = UserAgentParser.extractClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginResponse resp = socialSignupService.completeSocialSignup(request, ipAddress, userAgent);

        ResponseCookie refreshCookie = refreshCookieCreator.createRefreshCookie(
                resp.getRefreshToken(), resp.getRefreshExpiresIn());

        Map<String, Object> body = Map.of(
                "accessToken",   resp.getAccessToken(),
                "accessExpiresIn", resp.getAccessExpiresIn()
        );

        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    // ── inner DTO ────────────────────────────────────────────────────────────

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SocialPendingRequest {
        @NotBlank(message = "token은 필수입니다.")
        private String token;
    }
}
