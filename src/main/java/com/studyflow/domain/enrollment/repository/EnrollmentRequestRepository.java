package com.studyflow.domain.enrollment.repository;

import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EnrollmentRequestRepository extends JpaRepository<EnrollmentRequest, Long> {

    // 특정 상태의 신청 존재 여부 확인 — 주로 PENDING 중복 신청 방지에 사용
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentRequestStatus status);

    // 신청/취소를 반복했을 수 있으므로 가장 최근 신청 기준으로 myStatus 결정
    Optional<EnrollmentRequest> findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(Long userId, Long courseId);

    // 선생님 본인 수업에 대한 수강 신청 목록 조회 — courseId/status 필터 옵션
    @Query("""
            SELECT er FROM EnrollmentRequest er
            JOIN FETCH er.course c
            JOIN FETCH er.user u
            WHERE c.teacherProfile.id = :teacherProfileId
              AND (:courseId IS NULL OR c.id = :courseId)
              AND (:status IS NULL OR er.status = :status)
            ORDER BY er.createdAt DESC
            """)
    Page<EnrollmentRequest> findByTeacherProfileId(
            @Param("teacherProfileId") Long teacherProfileId,
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentRequestStatus status,
            Pageable pageable);

    // 학생 본인 수강 신청 목록 조회 — status 필터 옵션
    @Query("""
            SELECT er FROM EnrollmentRequest er
            JOIN FETCH er.course c
            JOIN FETCH c.teacherProfile tp
            JOIN FETCH tp.user
            WHERE er.user.id = :userId
              AND (:status IS NULL OR er.status = :status)
            ORDER BY er.createdAt DESC
            """)
    Page<EnrollmentRequest> findByUserId(
            @Param("userId") Long userId,
            @Param("status") EnrollmentRequestStatus status,
            Pageable pageable);
}
