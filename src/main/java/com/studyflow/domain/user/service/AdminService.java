package com.studyflow.domain.user.service;

import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.teacher.exception.VerificationNotFoundException;
import com.studyflow.domain.teacher.exception.VerificationNotPendingException;
import com.studyflow.domain.teacher.repository.TeacherVerificationRepository;
import com.studyflow.domain.user.dto.AdminVerificationDetailResponse;
import com.studyflow.domain.user.dto.AdminVerificationSummaryResponse;
import com.studyflow.domain.user.dto.RejectVerificationRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final TeacherVerificationRepository teacherVerificationRepository;

    // 선생님 인증 요청 목록 조회 — status가 null이면 전체 반환
    public Page<AdminVerificationSummaryResponse> getTeacherVerifications(
            VerificationStatus status, Pageable pageable) {
        return teacherVerificationRepository.findAllWithUser(status, pageable)
                .map(AdminVerificationSummaryResponse::from);
    }

    // 선생님 인증 요청 상세 조회
    public AdminVerificationDetailResponse getTeacherVerificationDetail(Long verificationId) {
        return teacherVerificationRepository.findByIdWithUser(verificationId)
                .map(AdminVerificationDetailResponse::from)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));
    }

    // 선생님 인증 요청 수락 — verification APPROVED + user.isVerified = true
    @Transactional
    public void approveVerification(Long verificationId) {
        TeacherVerification verification = teacherVerificationRepository.findByIdWithUser(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new VerificationNotPendingException();
        }

        verification.process(VerificationStatus.APPROVED, null);
        verification.getUser().verify();
    }

    // 선생님 인증 요청 거절
    @Transactional
    public void rejectVerification(Long verificationId, RejectVerificationRequest request) {
        TeacherVerification verification = teacherVerificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new VerificationNotPendingException();
        }

        verification.process(VerificationStatus.REJECTED, request.getRejectReason());
    }
}
