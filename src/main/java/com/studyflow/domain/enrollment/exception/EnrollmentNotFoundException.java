package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// 수강 기록을 찾을 수 없을 때 발생하는 예외
public class EnrollmentNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public EnrollmentNotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
