package com.studyflow.domain.teacher.repository;

import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeacherVerificationRepository extends JpaRepository<TeacherVerification, Long> {

    // 이미 심사 대기 중인 인증 요청 존재 여부 확인
    boolean existsByUserIdAndStatus(Long userId, VerificationStatus status);

    // 본인 인증 신청 목록 — 최신순 페이지네이션
    Page<TeacherVerification> findByUserId(Long userId, Pageable pageable);

    // 관리자 목록 조회 — User fetch join (teacherName 접근용), status 필터 옵션
    @Query(value = "SELECT v FROM TeacherVerification v JOIN FETCH v.user " +
                   "WHERE (:status IS NULL OR v.status = :status)",
           countQuery = "SELECT COUNT(v) FROM TeacherVerification v " +
                        "WHERE (:status IS NULL OR v.status = :status)")
    Page<TeacherVerification> findAllWithUser(
            @Param("status") VerificationStatus status, Pageable pageable);

    // 관리자 상세 조회 — User fetch join
    @Query("SELECT v FROM TeacherVerification v JOIN FETCH v.user WHERE v.id = :verificationId")
    java.util.Optional<TeacherVerification> findByIdWithUser(@Param("verificationId") Long verificationId);
}
