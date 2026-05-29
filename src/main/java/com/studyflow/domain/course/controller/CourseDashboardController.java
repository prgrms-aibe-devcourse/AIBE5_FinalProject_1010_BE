package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.dashboard.CourseDashboardResponse;
import com.studyflow.domain.course.service.CourseDashboardService;
import com.studyflow.domain.enrollment.dto.EnrolledStudentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// 수업별 페이지 대시보드 + 수강생 목록 API
// 로그인 + 해당 수업 참여자(선생님 또는 수강생)만 접근 가능
@RestController
@RequestMapping("/api/v1/courses/{courseId}")
@RequiredArgsConstructor
public class CourseDashboardController {

    private final CourseDashboardService dashboardService;

    // 수업별 페이지 상단 정보 — 수업명, 선생님 요약, 현재 수강 인원
    @GetMapping("/dashboard")
    public ResponseEntity<CourseDashboardResponse> getDashboard(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dashboardService.getDashboard(courseId, userId));
    }

    // 수강생 목록 — ACTIVE 상태 수강생만 반환
    @GetMapping("/enrollments")
    public ResponseEntity<List<EnrolledStudentResponse>> getEnrolledStudents(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(dashboardService.getEnrolledStudents(courseId, userId));
    }
}
