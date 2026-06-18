package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// 학생이 수강을 중도 포기하는 과정에서 발생하는 예외
public class EnrollmentDropException extends RuntimeException {
    private final ErrorCode errorCode;

    public EnrollmentDropException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
