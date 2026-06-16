package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomParticipantRepository extends JpaRepository<ClassroomParticipant, Long> {

    Optional<ClassroomParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);

    // 출석 집계 — 세션 목록 안에서 사용자별 참가 횟수 반환
    @Query("SELECT p.user.id AS userId, COUNT(p) AS count " +
           "FROM ClassroomParticipant p " +
           "WHERE p.session.id IN :sessionIds " +
           "GROUP BY p.user.id")
    List<UserAttendanceCount> countBySessionIds(@Param("sessionIds") List<Long> sessionIds);

    interface UserAttendanceCount {
        Long getUserId();
        Long getCount();
    }
}
