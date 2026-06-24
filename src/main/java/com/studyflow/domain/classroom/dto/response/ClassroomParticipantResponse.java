package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.classroom.entity.ClassroomParticipant;

import java.time.LocalDateTime;

// 강의실 참가(22-3) 응답 — 참가자 권한/미디어 상태 포함
public record ClassroomParticipantResponse(
        Long participantId,
        Long sessionId,
        Long userId,
        boolean canDraw,
        boolean canShareScreen,
        boolean canChat,
        boolean isVideoOn,
        boolean isAudioOn,
        LocalDateTime joinedAt
) {
    public static ClassroomParticipantResponse from(ClassroomParticipant p) {
        return new ClassroomParticipantResponse(
                p.getId(),
                p.getSession().getId(),
                p.getUser().getId(),
                p.isCanDraw(),
                p.isCanShareScreen(),
                p.isCanChat(),
                p.isVideoOn(),
                p.isAudioOn(),
                p.getJoinedAt()
        );
    }
}
