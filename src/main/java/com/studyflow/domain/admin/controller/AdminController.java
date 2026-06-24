package com.studyflow.domain.admin.controller;

import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.admin.dto.AdminCreditHistoryResponse;
import com.studyflow.domain.admin.dto.AdminCreditSummaryResponse;
import com.studyflow.domain.admin.dto.AdminVerificationDetailResponse;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.admin.dto.AdminVerificationSummaryResponse;
import com.studyflow.domain.admin.dto.AdminUserDetailInterface;
import com.studyflow.domain.admin.dto.AdminUserSummaryResponse;
import com.studyflow.domain.admin.dto.RejectVerificationRequest;
import com.studyflow.domain.admin.dto.CountResponse;
import com.studyflow.domain.admin.dto.UserCountByRoleResponse;
import com.studyflow.domain.admin.dto.UserCountStatisticsResponse;
import com.studyflow.domain.admin.service.AdminService;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.credit.dto.WithdrawalResponseDto;
import com.studyflow.domain.credit.enums.WithdrawalStatus;
import com.studyflow.domain.credit.repository.WithdrawalRequestRepository;
import com.studyflow.domain.credit.service.WithdrawalService;
import com.studyflow.domain.user.enums.UserRole;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
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
    private final WithdrawalService withdrawalService;

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
    // 예시: PATCH /api/v1/admin/teacher-verifications/1/reject
    //       Body: { "rejectReason": "사유" }  (선택)
    @PatchMapping("/teacher-verifications/{verificationId}/reject")
    public ResponseEntity<Void> rejectVerification(
            @PathVariable Long verificationId,
            @RequestBody(required = false) RejectVerificationRequest request) {
        String rejectReason = (request != null) ? request.getRejectReason() : null;
        adminService.rejectVerification(verificationId, rejectReason);
        return ResponseEntity.ok().build();
    }

    // 관리자 대시보드 - 가입자 수 확인 (활성화된 사용자) — 전체/학생/선생님/관리자
    // 예시: GET /api/v1/admin/dashboard/user-count
    @GetMapping("/dashboard/user-count")
    public ResponseEntity<UserCountByRoleResponse> getUserCount() {
        return ResponseEntity.ok(adminService.getUserCount());
    }

    // 관리자 대시보드 - 비활성 유저 수 확인 (isActive=false, 탈퇴 안 함) — 전체/학생/선생님/관리자
    // 예시: GET /api/v1/admin/dashboard/user-count/inactive
    @GetMapping("/dashboard/user-count/inactive")
    public ResponseEntity<UserCountByRoleResponse> getInactiveUserCount() {
        return ResponseEntity.ok(adminService.getInactiveUserCount());
    }

    // 관리자 대시보드 - 탈퇴 유저 수 확인 (isDeleted != 0) — 전체/학생/선생님/관리자
    // 예시: GET /api/v1/admin/dashboard/user-count/deleted
    @GetMapping("/dashboard/user-count/deleted")
    public ResponseEntity<UserCountByRoleResponse> getDeletedUserCount() {
        return ResponseEntity.ok(adminService.getDeletedUserCount());
    }

    // 관리자 대시보드 - 특정 날짜의 유저 수 통계 조회
    // 예시: GET /api/v1/admin/dashboard/statistics/2025-06-09
    @GetMapping("/dashboard/statistics/{date}")
    public ResponseEntity<UserCountStatisticsResponse> getUserCountStatistics(
            @PathVariable LocalDate date) {
        return ResponseEntity.ok(adminService.getUserCountStatistics(date));
    }

    // 관리자 대시보드 - 개설 수업 수 조회
    // 예시: GET /api/v1/admin/dashboard/course-count
    //       GET /api/v1/admin/dashboard/course-count?status=RECRUITING
    @GetMapping("/dashboard/course-count")
    public ResponseEntity<CountResponse> getCourseCount(
            @RequestParam(required = false) CourseStatus status) {
        return ResponseEntity.ok(adminService.getCourseCount(status));
    }

    // 관리자 대시보드 - 승인대기 선생님 수 조회
    // 예시: GET /api/v1/admin/dashboard/verification-pending-count
    @GetMapping("/dashboard/verification-pending-count")
    public ResponseEntity<CountResponse> getVerificationPendingCount() {
        return ResponseEntity.ok(adminService.getVerificationPendingCount());
    }

    // 관리자 페이지 - 활성 회원 목록 조회
    // 예시: GET /api/v1/admin/users?page=0&size=20
    //       GET /api/v1/admin/users?role=STUDENT&page=0&size=20
    @GetMapping("/users")
    public ResponseEntity<Page<AdminUserSummaryResponse>> getActiveUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getActiveUsers(role, pageable));
    }

    // 관리자 페이지 - 비활성 회원 목록 조회
    // 예시: GET /api/v1/admin/users/inactive?page=0&size=20
    @GetMapping("/users/inactive")
    public ResponseEntity<Page<AdminUserSummaryResponse>> getInactiveUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getInactiveUsers(role, pageable));
    }

    // 관리자 페이지 - 탈퇴 회원 목록 조회
    // 예시: GET /api/v1/admin/users/deleted?page=0&size=20
    @GetMapping("/users/deleted")
    public ResponseEntity<Page<AdminUserSummaryResponse>> getDeletedUsers(
            @RequestParam(required = false) UserRole role,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(adminService.getDeletedUsers(role, pageable));
    }

    // 관리자 페이지 - 회원 상세 정보 조회 (활성/비활성/탈퇴 무관)
    // 예시: GET /api/v1/admin/users/1
    @GetMapping("/users/{userId}")
    public ResponseEntity<AdminUserDetailInterface> getUserDetail(@PathVariable Long userId) {
        return ResponseEntity.ok(adminService.getUserDetail(userId));
    }

    // 관리자 페이지 - 결제/마일리지 내역 전체 조회
    // 예시: GET /api/v1/admin/credit-histories?startDate=2026-06-01&endDate=2026-06-30&email=test@test.com&reason=CHARGE&page=0&size=20
    @GetMapping("/credit-histories")
    public ResponseEntity<Page<AdminCreditHistoryResponse>> getCreditHistories(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) CreditReason reason,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(adminService.getCreditHistories(email, startDate, endDate, reason, pageable));
    }

    // 관리자 페이지 - 결제/마일리지 내역 통계 요약
    @GetMapping("/credit-histories/summary")
    public ResponseEntity<AdminCreditSummaryResponse> getCreditSummary(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) CreditReason reason) {
        return ResponseEntity.ok(adminService.getCreditSummary(email, startDate, endDate, reason));
    }

    // 마일리지 환급 신청 목록 조회
    // 예시: GET /api/v1/admin/withdrawals?status=PENDING&page=0&size=20
    @GetMapping("/withdrawals")
    public ResponseEntity<Page<WithdrawalResponseDto>> getWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(withdrawalService.getWithdrawals(status, pageable));
    }

    // 마일리지 환급 승인
    @PostMapping("/withdrawals/{id}/approve")
    public ResponseEntity<Void> approveWithdrawal(@PathVariable Long id) {
        withdrawalService.approveWithdrawal(id);
        return ResponseEntity.ok().build();
    }

    // 마일리지 환급 거절
    @PostMapping("/withdrawals/{id}/reject")
    public ResponseEntity<Void> rejectWithdrawal(@PathVariable Long id) {
        withdrawalService.rejectWithdrawal(id);
        return ResponseEntity.ok().build();
    }
}
