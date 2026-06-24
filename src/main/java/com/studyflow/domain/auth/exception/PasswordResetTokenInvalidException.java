package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;

public class PasswordResetTokenInvalidException extends RuntimeException {
    private final ErrorCode errorCode;

    public PasswordResetTokenInvalidException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
