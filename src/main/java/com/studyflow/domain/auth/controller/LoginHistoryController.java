package com.studyflow.domain.auth.controller;

import com.studyflow.domain.auth.dto.LoginHistoryResponse;
import com.studyflow.domain.auth.service.LoginHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth/login-history")
@RequiredArgsConstructor
public class LoginHistoryController {

    private final LoginHistoryService loginHistoryService;

    @GetMapping
    public ResponseEntity<Page<LoginHistoryResponse>> getMyLoginHistory(
            @AuthenticationPrincipal Long userId,
            @RequestParam(defaultValue = "0") int page) {
        return ResponseEntity.ok(loginHistoryService.getMyLoginHistory(userId, page));
    }
}
