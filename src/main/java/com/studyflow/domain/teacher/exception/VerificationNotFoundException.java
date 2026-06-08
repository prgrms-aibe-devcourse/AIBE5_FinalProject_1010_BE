package com.studyflow.domain.teacher.exception;

import com.studyflow.global.exception.ErrorCode;

public class VerificationNotFoundException extends RuntimeException {

    private final ErrorCode errorCode;

    public VerificationNotFoundException(Long verificationId) {
        super("인증 요청을 찾을 수 없습니다. id=" + verificationId);
        this.errorCode = ErrorCode.VERIFICATION_NOT_FOUND;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
