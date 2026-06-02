package com.studyflow.domain.enrollment.repository;

import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EnrollmentRequestRepository extends JpaRepository<EnrollmentRequest, Long> {

    // 특정 상태의 신청 존재 여부 확인 — 주로 PENDING 중복 신청 방지에 사용
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentRequestStatus status);

    // 신청/취소를 반복했을 수 있으므로 가장 최근 신청 기준으로 myStatus 결정
    Optional<EnrollmentRequest> findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(Long userId, Long courseId);
}
