package com.studyflow.domain.classroom.exception;

import com.studyflow.global.exception.ErrorCode;

// 존재하지 않는(또는 열린 세션이 없는) 강의실 조회/참가 시도 → 404
public class ClassroomSessionNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public ClassroomSessionNotFoundException(String message) {
        super(message);
        this.errorCode = ErrorCode.CLASSROOM_NOT_FOUND;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
