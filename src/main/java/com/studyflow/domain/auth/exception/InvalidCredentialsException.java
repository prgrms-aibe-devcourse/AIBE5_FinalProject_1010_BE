package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;

public class InvalidCredentialsException extends RuntimeException {
    private final ErrorCode errorCode;

    public InvalidCredentialsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
