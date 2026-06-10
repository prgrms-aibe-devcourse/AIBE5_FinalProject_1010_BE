package com.studyflow.domain.classroom.exception;

import com.studyflow.global.exception.ErrorCode;

// 이미 종료된 세션에 참가/종료를 시도하는 등 상태가 맞지 않을 때 → 400
public class ClassroomNotOpenException extends RuntimeException {
    private final ErrorCode errorCode;

    public ClassroomNotOpenException(String message) {
        super(message);
        this.errorCode = ErrorCode.CLASSROOM_NOT_OPEN;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
