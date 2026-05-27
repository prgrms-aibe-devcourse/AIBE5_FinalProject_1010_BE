package com.studyflow.domain.auth.controller;

import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.auth.service.AuthService;
import com.studyflow.global.auth.JwtTokenProvider;
import jakarta.validation.Valid;
import com.studyflow.domain.auth.dto.LoginRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/signup")
    public ResponseEntity<Void> signup(@Valid @RequestBody SignupRequest request) {
        // 필드 레벨 검증은 @Valid가 처리합니다.
        // 비밀번호와 비밀번호 확인이 일치하지 않으면 400 반환
        if (request.getPassword() == null || request.getPasswordConfirm() == null
                || !request.getPassword().equals(request.getPasswordConfirm())) {
            return ResponseEntity.badRequest().build();
        }
        // role이 STUDENT 또는 TEACHER가 아니면 400 반환
        if (!request.getRole().equals("STUDENT") && !request.getRole().equals("TEACHER")) {
            return ResponseEntity.badRequest().build();
        }
        // termsAgreement가 없거나, termsAgreement의 SERVICE와 PRIVACY가 false면 400 반환
        if (request.getTermsAgreements() == null || request.getTermsAgreements().size() != 3) {
            // 약관 항목이 3개가 아니면 에러
            return ResponseEntity.badRequest().build();
        }

        boolean serviceAgreed = false;
        boolean privacyAgreed = false;
        boolean marketingPresent = false;
        boolean marketingAgreed = false;
        for (SignupRequest.TermsAgreement ta : request.getTermsAgreements()) {
            if (ta.getTermsType() == SignupRequest.TermsType.SERVICE && ta.isAgreed()) {
                serviceAgreed = true;
            }
            if (ta.getTermsType() == SignupRequest.TermsType.PRIVACY && ta.isAgreed()) {
                privacyAgreed = true;
            }
            if (ta.getTermsType() == SignupRequest.TermsType.MARKETING) {
                // MARKETING은 동의 여부(true/false) 상관없이 항목 자체가 존재해야 함
                marketingPresent = true;
                marketingAgreed = ta.isAgreed();
            }
        }
        if (!serviceAgreed || !privacyAgreed || !marketingPresent) {
            return ResponseEntity.badRequest().build();
        }

        // 실제 가입 처리(서비스에 위임)
        authService.signup(request, marketingAgreed);
        return ResponseEntity.status(201).build();
    }

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // @Valid 검사 먼저 함
        // UserService로 넘어가서 로그인 검사
        LoginResponse resp = authService.login(request);

        // refresh token은 HttpOnly 쿠키로 전달
        boolean secure = !"local".equalsIgnoreCase(activeProfile);
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", resp.getRefreshToken())
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .maxAge(resp.getRefreshExpiresIn() / 1000)
                .sameSite("None")
                .build();

        // 응답 바디에는 access token과 만료시간만 전달
        Map<String, Object> body = Map.of(
                "accessToken", resp.getAccessToken(),
                "accessExpiresIn", resp.getAccessExpiresIn()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(body);
    }

    // 학생 인증 테스트용 api
    @GetMapping("/test/student")
    public ResponseEntity<String> studentTest() {
        return ResponseEntity.ok("학생 인증 성공");
    }

    // 선생님 인증 테스트용 api
    @GetMapping("/test/teacher")
    public ResponseEntity<String> teacherTest() {
        return ResponseEntity.ok("선생님 인증 성공");
    }
}
