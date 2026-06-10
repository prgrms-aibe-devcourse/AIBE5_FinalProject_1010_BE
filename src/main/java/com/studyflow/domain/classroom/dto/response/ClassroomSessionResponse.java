package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;

import java.time.LocalDateTime;

// 강의실 열기(22-1) 응답 — sessionId/courseId/status/startedAt/createdAt
public record ClassroomSessionResponse(
        Long sessionId,
        Long courseId,
        ClassroomStatus status,
        LocalDateTime startedAt,
        LocalDateTime createdAt
) {
    public static ClassroomSessionResponse from(ClassroomSession s) {
        return new ClassroomSessionResponse(
                s.getId(),
                s.getCourse().getId(),
                s.getStatus(),
                s.getStartedAt(),
                s.getCreatedAt()
        );
    }
}
