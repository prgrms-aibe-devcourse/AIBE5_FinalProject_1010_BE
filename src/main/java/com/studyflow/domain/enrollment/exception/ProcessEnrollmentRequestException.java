package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// 선생님이 수강신청을 처리(수락/거절)하는 과정에서 생기는 예외
public class ProcessEnrollmentRequestException extends RuntimeException {
    private final ErrorCode errorCode;

    public ProcessEnrollmentRequestException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
