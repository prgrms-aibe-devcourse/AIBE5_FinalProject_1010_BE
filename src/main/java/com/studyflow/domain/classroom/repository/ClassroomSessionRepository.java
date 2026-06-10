package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomSessionRepository extends JpaRepository<ClassroomSession, Long> {

    // 특정 수업의 현재 열린(OPEN) 세션 조회 — 열기 멱등성 보장 및 "현재 강의실 조회"에 사용.
    // 서비스가 OPEN 세션을 1개로 보장하지만, 데이터 이상 시에도 결정적으로 최신 세션을 고르도록 startedAt DESC 정렬.
    Optional<ClassroomSession> findTopByCourseIdAndStatusOrderByStartedAtDesc(Long courseId, ClassroomStatus status);
}
