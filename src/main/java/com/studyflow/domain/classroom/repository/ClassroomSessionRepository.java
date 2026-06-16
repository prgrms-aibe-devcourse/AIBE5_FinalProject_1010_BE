package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomSessionRepository extends JpaRepository<ClassroomSession, Long> {

    Optional<ClassroomSession> findTopByCourseIdAndStatusOrderByStartedAtDesc(Long courseId, ClassroomStatus status);

    // 출석 집계용 — 수업의 종료된 세션 ID 목록
    @Query("SELECT s.id FROM ClassroomSession s WHERE s.course.id = :courseId AND s.status = :status")
    List<Long> findSessionIdsByCourseIdAndStatus(@Param("courseId") Long courseId,
                                                 @Param("status") ClassroomStatus status);
}
