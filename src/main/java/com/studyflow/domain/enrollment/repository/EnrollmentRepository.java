package com.studyflow.domain.enrollment.repository;

import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {

    // 특정 사용자가 특정 수업에 해당 상태로 수강 중인지 빠르게 확인
    boolean existsByUserIdAndCourseIdAndStatus(Long userId, Long courseId, EnrollmentStatus status);

    // 특정 수업의 수강생 목록 (User 정보 포함 페치 — N+1 방지)
    @Query("SELECT e FROM Enrollment e JOIN FETCH e.user WHERE e.course.id = :courseId AND e.status = :status")
    List<Enrollment> findWithUserByCourseIdAndStatus(
            @Param("courseId") Long courseId,
            @Param("status") EnrollmentStatus status
    );

    // 수업별 페이지 대시보드에서 현재 수강 인원 수 표시용
    long countByCourseIdAndStatus(Long courseId, EnrollmentStatus status);

    // 여러 수업의 수강생 수 일괄 조회 — 검색 결과 목록에서 수업마다 개별 쿼리 날리는 N+1 방지
    // 반환: Object[]{ courseId(Long), count(Long) }
    // 수강생이 한 명도 없는 수업은 결과에 포함되지 않으므로 서비스에서 getOrDefault(0L) 처리 필요
    @Query("SELECT e.course.id, COUNT(e) " +
           "FROM Enrollment e " +
           "WHERE e.course.id IN :courseIds AND e.status = :status " +
           "GROUP BY e.course.id")
    List<Object[]> countByCourseIdsAndStatus(@Param("courseIds") List<Long> courseIds,
                                             @Param("status") EnrollmentStatus status);
}
