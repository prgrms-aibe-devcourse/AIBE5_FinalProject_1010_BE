package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ClassroomSessionRepository extends JpaRepository<ClassroomSession, Long> {

    Optional<ClassroomSession> findTopByCourseIdAndStatusOrderByStartedAtDesc(Long courseId, ClassroomStatus status);

    // 수업 삭제 가능 여부 확인 — 강의실 세션이 한 건이라도 있으면 삭제 불가
    boolean existsByCourseId(Long courseId);

    // 자동종료 스윕 — 호스트 하트비트가 cutoff보다 오래된 OPEN 세션(부재 의심)
    List<ClassroomSession> findByStatusAndLastHostSeenAtBefore(ClassroomStatus status, LocalDateTime cutoff);

    // 출석 집계용 — 수업의 종료된 세션 ID 목록
    @Query("SELECT s.id FROM ClassroomSession s WHERE s.course.id = :courseId AND s.status = :status")
    List<Long> findSessionIdsByCourseIdAndStatus(@Param("courseId") Long courseId,
                                                 @Param("status") ClassroomStatus status);

    // 메인 홈 "실시간 강의중" 공개 목록 — OPEN + 최근 하트비트(freshAfter 이후) + 노출 수업(isListed)만.
    // freshAfter로 호스트 부재 자동종료 전 좀비 세션을 거른다. course/teacher/user/subject를 JOIN FETCH해 N+1 방지.
    @Query("SELECT s FROM ClassroomSession s " +
           "JOIN FETCH s.course c " +
           "JOIN FETCH c.teacherProfile tp " +
           "JOIN FETCH tp.user u " +
           "JOIN FETCH c.subject subj " +
           "WHERE s.status = :status AND s.lastHostSeenAt >= :freshAfter AND c.isListed = true " +
           "ORDER BY s.startedAt DESC")
    List<ClassroomSession> findLiveSessions(@Param("status") ClassroomStatus status,
                                            @Param("freshAfter") LocalDateTime freshAfter,
                                            Pageable pageable);
}
