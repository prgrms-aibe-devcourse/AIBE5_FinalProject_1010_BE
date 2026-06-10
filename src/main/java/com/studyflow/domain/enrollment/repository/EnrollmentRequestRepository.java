package com.studyflow.domain.enrollment.repository;

import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface EnrollmentRequestRepository extends JpaRepository<EnrollmentRequest, Long> {

    // 특정 상태의 신청 존재 여부 확인 — 주로 PENDING 중복 신청 방지에 사용
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentRequestStatus status);

    // 수업 삭제 시 해당 수업의 PENDING 신청을 일괄 REJECTED 처리
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE EnrollmentRequest er SET er.status = 'REJECTED' WHERE er.course.id = :courseId AND er.status = 'PENDING'")
    int bulkRejectPendingByCourseId(@Param("courseId") Long courseId);

    // 수락/거절 처리용 — course·teacherProfile JOIN FETCH (소유권 검증 시 LAZY 추가 쿼리 방지)
    @Query("SELECT er FROM EnrollmentRequest er JOIN FETCH er.course c JOIN FETCH c.teacherProfile WHERE er.id = :id")
    Optional<EnrollmentRequest> findByIdWithCourse(@Param("id") Long id);

    // 취소 처리용 — user JOIN FETCH (본인 확인 시 LAZY 추가 쿼리 방지)
    @Query("SELECT er FROM EnrollmentRequest er JOIN FETCH er.user WHERE er.id = :id")
    Optional<EnrollmentRequest> findByIdWithUser(@Param("id") Long id);

    // 신청/취소를 반복했을 수 있으므로 가장 최근 신청 기준으로 myStatus 결정
    Optional<EnrollmentRequest> findFirstByUserIdAndCourseIdOrderByCreatedAtDesc(Long userId, Long courseId);

    // 선생님 본인 수업에 대한 수강 신청 목록 조회 — courseId/status 필터 옵션
    // countQuery를 분리해 JOIN FETCH + Page 조합에서 발생하는 count 오류 방지
    @Query(value = """
            SELECT er FROM EnrollmentRequest er
            JOIN FETCH er.course c
            JOIN FETCH er.user u
            WHERE c.teacherProfile.id = :teacherProfileId
              AND (:courseId IS NULL OR c.id = :courseId)
              AND (:status IS NULL OR er.status = :status)
            ORDER BY er.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(er) FROM EnrollmentRequest er
            JOIN er.course c
            WHERE c.teacherProfile.id = :teacherProfileId
              AND (:courseId IS NULL OR c.id = :courseId)
              AND (:status IS NULL OR er.status = :status)
            """)
    Page<EnrollmentRequest> findByTeacherProfileId(
            @Param("teacherProfileId") Long teacherProfileId,
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentRequestStatus status,
            Pageable pageable);

    // 학생 본인 수강 신청 목록 조회 — status 필터 옵션
    // countQuery를 분리해 JOIN FETCH + Page 조합에서 발생하는 count 오류 방지
    @Query(value = """
            SELECT er FROM EnrollmentRequest er
            JOIN FETCH er.course c
            JOIN FETCH c.teacherProfile tp
            JOIN FETCH tp.user
            WHERE er.user.id = :userId
              AND (:status IS NULL OR er.status = :status)
            ORDER BY er.createdAt DESC
            """,
           countQuery = """
            SELECT COUNT(er) FROM EnrollmentRequest er
            WHERE er.user.id = :userId
              AND (:status IS NULL OR er.status = :status)
            """)
    Page<EnrollmentRequest> findByUserId(
            @Param("userId") Long userId,
            @Param("status") EnrollmentRequestStatus status,
            Pageable pageable);
}
