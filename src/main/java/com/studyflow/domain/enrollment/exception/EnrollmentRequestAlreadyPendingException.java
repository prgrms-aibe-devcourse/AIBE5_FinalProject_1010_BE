package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// PENDING 신청이 이미 있는데 같은 수업에 다시 신청할 때 발생 → 409
public class EnrollmentRequestAlreadyPendingException extends RuntimeException {
    public EnrollmentRequestAlreadyPendingException() {
        super(ErrorCode.ENROLLMENT_REQUEST_ALREADY_PENDING.getMessage());
    }
}
