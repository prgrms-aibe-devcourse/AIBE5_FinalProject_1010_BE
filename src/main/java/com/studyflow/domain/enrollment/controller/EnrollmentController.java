package com.studyflow.domain.enrollment.controller;

import com.studyflow.domain.enrollment.service.EnrollmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/enrollments")
@RequiredArgsConstructor
public class EnrollmentController {

    private final EnrollmentService enrollmentService;

    // 수강 중도 포기 (본인 ACTIVE 수강에 한함)
    @PatchMapping("/{enrollmentId}/drop")
    public ResponseEntity<Void> dropEnrollment(@PathVariable Long enrollmentId,
                                               @AuthenticationPrincipal Long userId) {
        enrollmentService.dropEnrollment(enrollmentId, userId);
        return ResponseEntity.noContent().build();
    }
}
