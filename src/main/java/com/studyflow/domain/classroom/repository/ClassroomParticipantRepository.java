package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClassroomParticipantRepository extends JpaRepository<ClassroomParticipant, Long> {

    Optional<ClassroomParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);

    // 판서 권한이 있는 참가자의 userId 목록 — 화이트보드 인메모리 권한 캐시 로딩용(이슈 #162).
    // p.user.id 는 FK 컬럼을 그대로 읽어 user 테이블 조인이 발생하지 않는다.
    @Query("SELECT p.user.id FROM ClassroomParticipant p WHERE p.session.id = :sessionId AND p.canDraw = true")
    List<Long> findDrawerUserIdsBySessionId(@Param("sessionId") Long sessionId);

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
