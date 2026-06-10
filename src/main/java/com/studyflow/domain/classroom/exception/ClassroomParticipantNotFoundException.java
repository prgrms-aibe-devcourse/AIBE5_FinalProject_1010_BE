package com.studyflow.domain.classroom.exception;

import com.studyflow.global.exception.ErrorCode;

// 존재하지 않는 강의실 참가자에 대한 조회/권한변경 시도 → 404
public class ClassroomParticipantNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public ClassroomParticipantNotFoundException(Long participantId) {
        super("강의실 참가자를 찾을 수 없습니다. (participantId: " + participantId + ")");
        this.errorCode = ErrorCode.CLASSROOM_PARTICIPANT_NOT_FOUND;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
