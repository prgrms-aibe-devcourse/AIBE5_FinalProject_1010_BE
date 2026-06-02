package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// 이미 ACTIVE 상태로 수강 중인 수업에 재신청할 때 발생 → 409
public class AlreadyEnrolledException extends RuntimeException {
    public AlreadyEnrolledException() {
        super(ErrorCode.ALREADY_ENROLLED.getMessage());
    }
}
