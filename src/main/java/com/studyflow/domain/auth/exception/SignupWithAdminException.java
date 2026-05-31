package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;

public class SignupWithAdminException extends RuntimeException {
    private final ErrorCode errorCode;

    public SignupWithAdminException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
