package com.studyflow.domain.student.controller;

import com.studyflow.domain.student.dto.StudentProfileResponse;
import com.studyflow.domain.student.dto.StudentProfileUpdateRequest;
import com.studyflow.domain.student.service.StudentService;
import com.studyflow.domain.teacher.exception.ProfileAuthInfoException;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.auth.controllerutil.CheckAuthInController;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;

    // 학생 본인 프로필 조회
    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId,
                                          Authentication authentication) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.STUDENT,
                ProfileAuthInfoException::new);

        StudentProfileResponse response = studentService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 학생 본인 프로필 수정
    @PatchMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal Long userId,
                                             Authentication authentication,
                                             @RequestBody StudentProfileUpdateRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.STUDENT,
                ProfileAuthInfoException::new);

        StudentProfileResponse response = studentService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }
}
