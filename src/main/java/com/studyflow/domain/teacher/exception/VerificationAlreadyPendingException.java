package com.studyflow.domain.teacher.exception;

import com.studyflow.global.exception.ErrorCode;

public class VerificationAlreadyPendingException extends RuntimeException {

    private final ErrorCode errorCode;

    public VerificationAlreadyPendingException() {
        super(ErrorCode.VERIFICATION_ALREADY_PENDING.getMessage());
        this.errorCode = ErrorCode.VERIFICATION_ALREADY_PENDING;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
