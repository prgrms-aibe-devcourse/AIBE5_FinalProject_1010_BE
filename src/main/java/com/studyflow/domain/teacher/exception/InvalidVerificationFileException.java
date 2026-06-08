package com.studyflow.domain.teacher.exception;

import com.studyflow.global.exception.ErrorCode;

public class InvalidVerificationFileException extends RuntimeException {
    private final ErrorCode errorCode;

    public InvalidVerificationFileException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
