package com.studyflow.domain.user.controller;

import com.studyflow.domain.auth.service.AuthService;
import com.studyflow.domain.user.service.UserService;
import com.studyflow.global.auth.RefreshCookieCreator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
    private final RefreshCookieCreator refreshCookieCreator;

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
