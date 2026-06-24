package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;

import java.time.LocalDateTime;

// 현재 강의실 조회(22-2) 응답 — endedAt/durationSeconds 포함(OPEN이면 둘 다 null)
public record ClassroomCurrentResponse(
        Long sessionId,
        Long courseId,
        ClassroomStatus status,
        LocalDateTime startedAt,
        LocalDateTime endedAt,
        Long durationSeconds
) {
    public static ClassroomCurrentResponse from(ClassroomSession s) {
        return new ClassroomCurrentResponse(
                s.getId(),
                s.getCourse().getId(),
                s.getStatus(),
                s.getStartedAt(),
                s.getEndedAt(),
                s.getDurationSeconds()
        );
    }
}
