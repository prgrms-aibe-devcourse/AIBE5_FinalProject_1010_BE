package com.studyflow.domain.student.controller;

import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.student.dto.StudentEnrolledCourseResponse;
import com.studyflow.domain.student.dto.StudentEnrollmentRequestResponse;
import com.studyflow.domain.student.dto.StudentProfileResponse;
import com.studyflow.domain.student.dto.StudentProfileUpdateRequest;
import com.studyflow.domain.student.service.StudentService;
import com.studyflow.global.exception.ProfileAuthInfoException;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.auth.controllerutil.CheckAuthInController;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Validated
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
                                             @Valid @RequestBody StudentProfileUpdateRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.STUDENT,
                ProfileAuthInfoException::new);

        StudentProfileResponse response = studentService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 학생 본인 수강 수업 목록 조회
    // 예시: GET /api/v1/students/me/enrollments?status=ACTIVE&page=0&size=12
    @GetMapping("/me/enrollments")
    public ResponseEntity<Page<StudentEnrolledCourseResponse>> getMyCourses(
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam(required = false) EnrollmentStatus status,
            @PageableDefault(size = 12) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.STUDENT,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(studentService.getMyCourses(userId, status, pageable));
    }

    // 학생 본인 수강신청 목록 조회
    // 예시: GET /api/v1/students/me/enrollment-requests?status=PENDING&page=0&size=12
    @GetMapping("/me/enrollment-requests")
    public ResponseEntity<Page<StudentEnrollmentRequestResponse>> getEnrollmentRequests(
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam(required = false) EnrollmentRequestStatus status,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.STUDENT,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(studentService.getMyEnrollmentRequests(userId, status, pageable));
    }
}
