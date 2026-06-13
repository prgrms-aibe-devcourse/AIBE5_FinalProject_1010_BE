package com.studyflow.domain.auth.controller;

import com.studyflow.domain.auth.dto.*;
import com.studyflow.domain.auth.exception.SignupRequestException;
import com.studyflow.domain.auth.service.AuthService;
import com.studyflow.domain.auth.dto.SignupRequest.TermsType;
import com.studyflow.global.auth.RefreshCookieCreator;
import com.studyflow.global.exception.ErrorCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final RefreshCookieCreator refreshCookieCreator;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // 비밀번호 재설정 링크 발송
    @PostMapping("/password/reset/link")
    public ResponseEntity<?> sendPasswordResetLink(@Valid @RequestBody PasswordResetLinkRequest request) {
        authService.sendPasswordResetLink(request);
        return ResponseEntity.ok().build();
    }

    // 비밀번호 재설정 적용
    @PostMapping("/password/reset")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().build();
    }

    // 이메일 인증 코드 발송
    @PostMapping("/email/code/send")
    public ResponseEntity<?> sendAuthCode(@Valid @RequestBody EmailAuthRequest request) {
        authService.sendAuthCode(request);
        return ResponseEntity.ok().build();
    }

    // 이메일 인증 코드 확인 — 성공 시 회원가입에 사용할 단회용 검증 토큰 반환
    @PostMapping("/email/verify")
    public ResponseEntity<EmailVerifyResponse> verifyAuthCode(@Valid @RequestBody EmailVerifyRequest request) {
        return ResponseEntity.ok(authService.verifyAuthCode(request));
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        // 필드 레벨 검증은 @Valid가 처리합니다.
        // 비밀번호와 비밀번호 확인이 일치하지 않으면 400 반환
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new SignupRequestException(
                    ErrorCode.VALIDATION_ERROR,
                    "비밀번호와 비밀번호 확인이 일치하지 않습니다.");
        }

        // role 정보는 Service layer에서 검증

        // termsAgreement가 없거나, termsAgreement의 SERVICE와 PRIVACY가 false면 400 반환
        if (request.getTermsAgreements() == null || request.getTermsAgreements().size() != TermsType.values().length) {
            // 약관 항목이 3개가 아니면 에러
            throw new SignupRequestException(
                    ErrorCode.VALIDATION_ERROR,
                    "약관 동의 정보가 유효하지 않습니다.");
        }

        // 실제 가입 처리(서비스에 위임)
        authService.signup(request);
        return ResponseEntity.status(201).build();
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // @Valid 검사 먼저 함
        // UserService로 넘어가서 로그인 검사
        LoginResponse resp;
        try {
            resp = authService.login(request);
        } catch (IllegalStateException e) {
            /*
            Role이 Spring Security에서 확인되었으나, 서비스 레벨에서 확인되지 않는 경우
            request 데이터 무결성 파괴 - 403과 구분
             */
            // body는 나중에 작성 예정
            return ResponseEntity.status(422).build();
        }

        ResponseCookie refreshCookie = refreshCookieCreator.createRefreshCookie(
                resp.getRefreshToken(), resp.getRefreshExpiresIn());

        Map<String, Object> body = new HashMap<>();
        body.put("userId",          resp.getUserId());
        body.put("name",            resp.getName());
        body.put("role",            resp.getRole());
        body.put("accessToken",     resp.getAccessToken());
        body.put("accessExpiresIn", resp.getAccessExpiresIn());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    // refresh token을 통한 (access token, refresh token) 재발급
    // RTR 전략 채택
    @PostMapping("/reissue")
    public ResponseEntity<?> refresh(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                     @AuthenticationPrincipal Long userId) {
        if(refreshToken == null || userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }
        ReissueResponse reissueResponse;
        try {
            reissueResponse = authService.reissue(refreshToken, userId);
        } catch(IllegalStateException e) {
            // 인증된 사용자의 권한 정보가 없는 이상한 경우
            // 컴파일 및 디버깅을 위함
            return ResponseEntity.status(422).build();
        }

        ResponseCookie refreshCookie = refreshCookieCreator.createRefreshCookie(
                reissueResponse.getRefreshToken(), reissueResponse.getRefreshExpiresIn());

        Map<String, Object> body = new HashMap<>();
        body.put("userId",          reissueResponse.getUserId());
        body.put("name",            reissueResponse.getName());
        body.put("role",            reissueResponse.getRole());
        body.put("accessToken",     reissueResponse.getAccessToken());
        body.put("accessExpiresIn", reissueResponse.getAccessExpiresIn());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@AuthenticationPrincipal Long userId) {
        if(userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }
        // refresh token의 만료시간을 0으로 설정하여 재발급
        ResponseCookie deleteCookie = refreshCookieCreator.createRefreshCookie("",0);

        authService.logout(userId);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }

}
