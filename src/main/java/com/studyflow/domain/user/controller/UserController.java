package com.studyflow.domain.user.controller;

import com.studyflow.domain.auth.service.AuthService;
import com.studyflow.domain.user.dto.PasswordChangeRequest;
import com.studyflow.domain.user.dto.UserUpdateRequest;
import com.studyflow.domain.user.service.UserService;
import com.studyflow.global.auth.RefreshCookieCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import com.studyflow.domain.user.dto.UserInfoResponse;
import com.studyflow.domain.user.dto.VoiceCallSettingRequest;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RefreshCookieCreator refreshCookieCreator;

    // 회원 정보 조회
    @GetMapping("/me")
    public ResponseEntity<?> getUser(@AuthenticationPrincipal Long userId) {
        UserInfoResponse response = userService.getUser(userId);
        return ResponseEntity.ok(response);
    }

    // 회원정보 수정
    @PatchMapping("/me")
    public ResponseEntity<?> updateUser(@AuthenticationPrincipal Long userId,
                                        @Valid @RequestBody UserUpdateRequest request) {
        if(userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }

        userService.updateUser(userId, request);
        return ResponseEntity.ok().build();
    }

    // 보이스톡 수신 설정 변경
    @PatchMapping("/me/voice-call-setting")
    public ResponseEntity<?> updateVoiceCallSetting(@AuthenticationPrincipal Long userId,
                                                    @Valid @RequestBody VoiceCallSettingRequest request) {
        if(userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }

        userService.updateVoiceCallSetting(userId, request.isVoiceCallEnabled());
        return ResponseEntity.ok().build();
    }

    // 비밀번호 변경
    @PatchMapping("/me/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        if (userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }

        userService.changePassword(userId, request);
        return ResponseEntity.ok().build();
    }

    // 회원 탈퇴
    @DeleteMapping("/me")
    public ResponseEntity<?> deleteUser(@AuthenticationPrincipal Long userId) {
        if(userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }

        userService.deleteUser(userId);

        ResponseCookie deleteCookie = refreshCookieCreator.createRefreshCookie("",0);

        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .build();
    }
}
