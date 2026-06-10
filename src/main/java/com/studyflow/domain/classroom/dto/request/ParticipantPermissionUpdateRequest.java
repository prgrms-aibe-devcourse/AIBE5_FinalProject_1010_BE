package com.studyflow.domain.classroom.dto.request;

import jakarta.validation.constraints.NotNull;

/**
 * 참가자 권한 변경(22-5) 요청 — 선생님이 학생의 판서/화면공유/채팅/송출 권한을 조정.
 *
 * <p>canPublish는 선택값(null이면 기존 값 유지). 송출 게이팅 — 발표시킬 학생에게만 true.</p>
 */
public record ParticipantPermissionUpdateRequest(
        @NotNull Boolean canDraw,
        @NotNull Boolean canShareScreen,
        @NotNull Boolean canChat,
        Boolean canPublish
) {
}
