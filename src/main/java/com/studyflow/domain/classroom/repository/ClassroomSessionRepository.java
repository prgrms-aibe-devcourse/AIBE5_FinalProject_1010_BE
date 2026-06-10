package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomSessionRepository extends JpaRepository<ClassroomSession, Long> {

    // 특정 수업의 현재 열린(OPEN) 세션 조회 — 열기 멱등성 보장 및 "현재 강의실 조회"에 사용
    Optional<ClassroomSession> findFirstByCourseIdAndStatus(Long courseId, ClassroomStatus status);
}
