package com.studyflow.domain.admin.exception;

import com.studyflow.global.exception.ErrorCode;

public class VerificationNotPendingException extends RuntimeException {

    private final ErrorCode errorCode;

    public VerificationNotPendingException() {
        super(ErrorCode.VERIFICATION_NOT_PENDING.getMessage());
        this.errorCode = ErrorCode.VERIFICATION_NOT_PENDING;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
