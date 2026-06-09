package com.studyflow.domain.admin.controller;

import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.admin.dto.AdminVerificationDetailResponse;
import com.studyflow.domain.admin.dto.AdminVerificationSummaryResponse;
import com.studyflow.domain.admin.service.AdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 선생님 인증요청 목록 조회
    // 예시: GET /api/v1/admin/teacher-verifications?status=PENDING&page=0&size=12
    @GetMapping("/teacher-verifications")
    public ResponseEntity<Page<AdminVerificationSummaryResponse>> getTeacherVerifications(
            @RequestParam(required = false) VerificationStatus status,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getTeacherVerifications(status, pageable));
    }

    // 선생님 인증요청 상세 조회
    // 예시: GET /api/v1/admin/teacher-verifications/1
    @GetMapping("/teacher-verifications/{verificationId}")
    public ResponseEntity<AdminVerificationDetailResponse> getTeacherVerificationDetail(
            @PathVariable Long verificationId) {
        return ResponseEntity.ok(adminService.getTeacherVerificationDetail(verificationId));
    }

    // 선생님 인증요청 수락
    // 예시: PATCH /api/v1/admin/teacher-verifications/1/approve
    @PatchMapping("/teacher-verifications/{verificationId}/approve")
    public ResponseEntity<Void> approveVerification(@PathVariable Long verificationId) {
        adminService.approveVerification(verificationId);
        return ResponseEntity.ok().build();
    }

    // 선생님 인증요청 거절
    // 예시: PATCH /api/v1/admin/teacher-verifications/1/reject?rejectReason=사유
    @PatchMapping("/teacher-verifications/{verificationId}/reject")
    public ResponseEntity<Void> rejectVerification(
            @PathVariable Long verificationId,
            @RequestParam(required = false) String rejectReason) {
        adminService.rejectVerification(verificationId, rejectReason);
        return ResponseEntity.ok().build();
    }
}
