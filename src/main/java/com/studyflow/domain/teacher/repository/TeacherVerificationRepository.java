package com.studyflow.domain.teacher.repository;

import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeacherVerificationRepository extends JpaRepository<TeacherVerification, Long> {

    // 이미 심사 대기 중인 인증 요청 존재 여부 확인
    boolean existsByUserIdAndStatus(Long userId, VerificationStatus status);

    // 본인 인증 신청 목록 — 최신순 페이지네이션
    Page<TeacherVerification> findByUserId(Long userId, Pageable pageable);
}
