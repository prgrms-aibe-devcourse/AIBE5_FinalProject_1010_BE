package com.studyflow.domain.auth.controller;

import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.SocialPendingInfoResponse;
import com.studyflow.domain.auth.dto.SocialSignupRequest;
import com.studyflow.domain.auth.service.SocialSignupService;
import com.studyflow.global.auth.RefreshCookieCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import java.util.Map;

/**
 * 소셜 로그인 추가 정보 입력 관련 엔드포인트.
 * - GET  /api/v1/auth/social-pending?token=...  : 폼 pre-fill용 소셜 데이터 조회
 * - POST /api/v1/auth/social-signup              : 추가 정보 입력 후 가입 완료
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
     * 소셜 미제공 필드(gender, birthDate, phone)는 null로 내려오며, FE에서 빈 칸으로 표시합니다.
     */
    @GetMapping("/social-pending")
    public ResponseEntity<SocialPendingInfoResponse> getSocialPendingInfo(@RequestParam String token) {
        return ResponseEntity.ok(socialSignupService.getPendingInfo(token));
    }

    @PostMapping("/social-signup")
    public ResponseEntity<?> socialSignup(@RequestBody SocialSignupRequest request) {
        LoginResponse resp = socialSignupService.completeSocialSignup(request);

        // refresh token은 httpOnly 쿠키로 전달
        ResponseCookie refreshCookie = refreshCookieCreator.createRefreshCookie(
                resp.getRefreshToken(), resp.getRefreshExpiresIn());

        // 응답 바디에는 access token만 전달
        Map<String, Object> body = Map.of(
                "accessToken", resp.getAccessToken(),
                "accessExpiresIn", resp.getAccessExpiresIn()
        );

        return ResponseEntity.status(201)
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }
}
