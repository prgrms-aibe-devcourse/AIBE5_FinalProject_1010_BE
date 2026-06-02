package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.detail.CourseDetailResponse;
import com.studyflow.domain.course.service.CourseDetailService;
import com.studyflow.domain.enrollment.dto.EnrollmentRequestCreateRequest;
import com.studyflow.domain.enrollment.dto.EnrollmentRequestResponse;
import com.studyflow.domain.enrollment.service.EnrollmentRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 수업 상세 조회 + 수강 신청 API
// GET  /{courseId}                      — 비로그인 포함 공개, 토큰 있으면 myStatus 포함
// POST /{courseId}/enrollment-requests  — 학생 전용 (SecurityConfig hasRole STUDENT)
@Tag(name = "수업 상세", description = "수업 상세 조회 및 수강 신청 API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseDetailController {

    private final CourseDetailService courseDetailService;
    private final EnrollmentRequestService enrollmentRequestService;

    // 수업 상세 조회 — userId는 비로그인 시 null, role은 myStatus 계산에 사용
    @Operation(summary = "수업 상세 조회", description = "비로그인 포함 공개. 로그인 시 myStatus(OWNER/TEACHER/ENROLLED/PENDING/REJECTED/CANCELLED/NOT_APPLIED) 포함")
    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Parameter(hidden = true) Authentication authentication
    ) {
        // 로그인 상태일 때만 role 추출 (비로그인이면 authentication이 null)
        String role = (authentication != null && userId != null)
                ? authentication.getAuthorities().iterator().next()
                        .getAuthority().replace("ROLE_", "")
                : null;

        return ResponseEntity.ok(courseDetailService.getCourseDetail(courseId, userId, role));
    }

    // 수강 신청 — 완료 시 선생님-학생 채팅방 자동 개설, 응답에 chatRoomId 포함
    @Operation(summary = "수강 신청", description = "학생 전용. 신청 완료 시 선생님-학생 채팅방 자동 개설. 응답의 chatRoomId로 채팅창 이동 가능")
    @PostMapping("/{courseId}/enrollment-requests")
    public ResponseEntity<EnrollmentRequestResponse> createEnrollmentRequest(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody EnrollmentRequestCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(enrollmentRequestService.createEnrollmentRequest(courseId, userId, request));
    }
}
