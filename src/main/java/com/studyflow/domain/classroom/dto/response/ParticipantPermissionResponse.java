package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.classroom.entity.ClassroomParticipant;

// 참가자 권한 변경(22-5) 응답
public record ParticipantPermissionResponse(
        Long participantId,
        boolean canDraw,
        boolean canShareScreen,
        boolean canChat,
        boolean canPublish
) {
    public static ParticipantPermissionResponse from(ClassroomParticipant p) {
        return new ParticipantPermissionResponse(
                p.getId(),
                p.isCanDraw(),
                p.isCanShareScreen(),
                p.isCanChat(),
                p.isCanPublish()
        );
    }
}
