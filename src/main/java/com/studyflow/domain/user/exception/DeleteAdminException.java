package com.studyflow.domain.user.exception;

import com.studyflow.global.exception.ErrorCode;

public class DeleteAdminException extends RuntimeException {
    private final ErrorCode errorCode;

    public DeleteAdminException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
