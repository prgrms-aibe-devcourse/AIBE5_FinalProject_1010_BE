package com.studyflow.domain.student.controller;

import com.studyflow.domain.student.dto.StudentProfileResponse;
import com.studyflow.domain.student.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // 학생 본인 프로필 조회
    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId,
                                          Authentication authentication) {
        String role = null;
        // role 정보 추출
        if(authentication != null) {
            role = authentication.getAuthorities().stream()
                    .findFirst()
                    .map(GrantedAuthority::getAuthority)  // "ROLE_TEACHER"
                    .orElse(null);
        }

        // 인증 정보가 없거나, 학생 회원이 아닌 경우
        if (userId == null || role == null || !role.equals("ROLE_STUDENT")) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }

        StudentProfileResponse response = studentService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }
}
