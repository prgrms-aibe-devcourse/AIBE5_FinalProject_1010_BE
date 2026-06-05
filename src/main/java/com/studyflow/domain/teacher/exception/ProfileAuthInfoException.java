package com.studyflow.domain.teacher.exception;

import com.studyflow.global.exception.ErrorCode;

public class ProfileAuthInfoException extends RuntimeException {
    private final ErrorCode errorCode;

    public ProfileAuthInfoException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
