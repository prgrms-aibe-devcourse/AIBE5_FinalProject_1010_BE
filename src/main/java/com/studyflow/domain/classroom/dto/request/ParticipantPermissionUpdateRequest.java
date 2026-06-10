package com.studyflow.domain.classroom.dto.request;

import jakarta.validation.constraints.NotNull;

// 참가자 권한 변경(22-5) 요청 — 선생님이 학생의 판서/화면공유/채팅 권한을 조정
public record ParticipantPermissionUpdateRequest(
        @NotNull Boolean canDraw,
        @NotNull Boolean canShareScreen,
        @NotNull Boolean canChat
) {
}
