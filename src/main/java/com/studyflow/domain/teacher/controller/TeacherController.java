package com.studyflow.domain.teacher.controller;

import com.studyflow.domain.teacher.dto.*;

import java.util.Map;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.global.exception.ProfileAuthInfoException;
import com.studyflow.domain.teacher.service.TeacherService;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.auth.controllerutil.CheckAuthInController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// 선생님 목록 및 상세 API — 비로그인 포함 전체 공개
@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Validated
public class TeacherController {

    private final TeacherService teacherService;

    // 선생님 목록 — 검색/필터 지원
    // 예시: GET /api/v1/teachers?keyword=홍길동&minNaegong=500&page=0&size=12
    @GetMapping
    public ResponseEntity<Page<TeacherCardResponse>> getTeacherList(
            @RequestParam(required = false) String keyword,
            @PositiveOrZero(message = "내공 점수 하한은 0 이상이어야 합니다.")
            @RequestParam(required = false) Integer minNaegong,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(teacherService.getTeacherList(keyword, minNaegong, pageable));
    }

    // 선생님 상세 페이지
    // 예시: GET /api/v1/teachers/1
    @GetMapping("/{teacherProfileId}")
    public ResponseEntity<TeacherDetailResponse> getTeacherDetail(@PathVariable Long teacherProfileId) {
        return ResponseEntity.ok(teacherService.getTeacherDetail(teacherProfileId));
    }

    // 선생님 마이페이지 관련 api

    // 로그인한 선생님 본인의 프로필 조회
    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId,
                                          Authentication authentication) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        TeacherProfileResponse response = teacherService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 로그인한 선생님 본인의 프로필 수정
    @PatchMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal Long userId,
                                             Authentication authentication,
                                             @Valid @RequestBody TeacherProfileUpdateRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        TeacherProfileResponse response = teacherService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 로그인한 선생님 본인의 수업 목록 조회
    // 예시: GET /api/v1/teachers/me/courses?status=RECRUITING&page=0&size=12
    @GetMapping("/me/courses")
    public ResponseEntity<Page<TeacherCourseCardResponse>> getMyCourses(@AuthenticationPrincipal Long userId,
                                                                        Authentication authentication,
                                                                        @RequestParam(required = false) CourseStatus status,
                                                                        @PageableDefault(size = 12) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(teacherService.getMyCourses(userId, status, pageable));
    }

    // 본인의 수업에 대한 수강 신청 목록 조회
    // 예시: GET /api/v1/teachers/me/enrollment-requests?courseId=10&status=PENDING&page=0&size=12
    @GetMapping("/me/enrollment-requests")
    public ResponseEntity<Page<EnrollmentRequestSummaryResponse>> getEnrollmentRequests(
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) EnrollmentRequestStatus status,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(teacherService.getEnrollmentRequests(userId, courseId, status, pageable));
    }

    // 선생님 인증 요청
    // 예시: POST /api/v1/teachers/me/verifications
    @PostMapping("/me/verifications")
    public ResponseEntity<Map<String, Long>> requestVerification(@AuthenticationPrincipal Long userId,
                                                                 Authentication authentication,
                                                                 @Valid @RequestBody TeacherVerificationRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        Long verificationId = teacherService.requestVerification(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", verificationId));
    }

    // 선생님 본인 인증 신청 목록 조회
    // 예시: GET /api/v1/teachers/me/verifications?page=0&size=12
    @GetMapping("/me/verifications")
    public ResponseEntity<Page<TeacherVerificationResponse>> getVerificationList(
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(teacherService.getMyVerifications(userId, pageable));
    }
}
