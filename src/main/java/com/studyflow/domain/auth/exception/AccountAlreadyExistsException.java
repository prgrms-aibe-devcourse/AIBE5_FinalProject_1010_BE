package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;

public class AccountAlreadyExistsException extends RuntimeException {
    private final ErrorCode errorCode;

    public AccountAlreadyExistsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
