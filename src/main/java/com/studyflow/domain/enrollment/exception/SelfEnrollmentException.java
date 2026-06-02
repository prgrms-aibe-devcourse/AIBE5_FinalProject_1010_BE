package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// 선생님이 본인 수업에 수강 신청할 때 발생 → 400
public class SelfEnrollmentException extends RuntimeException {
    public SelfEnrollmentException() {
        super(ErrorCode.SELF_ENROLLMENT.getMessage());
    }
}
