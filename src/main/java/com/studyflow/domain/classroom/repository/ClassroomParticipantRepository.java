package com.studyflow.domain.classroom.repository;

import com.studyflow.domain.classroom.entity.ClassroomParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClassroomParticipantRepository extends JpaRepository<ClassroomParticipant, Long> {

    // 재입장 시 기존 참가 행 재사용 — (세션, 사용자) 유일
    Optional<ClassroomParticipant> findBySessionIdAndUserId(Long sessionId, Long userId);
}
