package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;

import java.time.LocalDateTime;

// 강의실 종료(22-6) 응답 — sessionId/status/endedAt/durationSeconds
public record ClassroomCloseResponse(
        Long sessionId,
        ClassroomStatus status,
        LocalDateTime endedAt,
        Long durationSeconds
) {
    public static ClassroomCloseResponse from(ClassroomSession s) {
        return new ClassroomCloseResponse(
                s.getId(),
                s.getStatus(),
                s.getEndedAt(),
                s.getDurationSeconds()
        );
    }
}
