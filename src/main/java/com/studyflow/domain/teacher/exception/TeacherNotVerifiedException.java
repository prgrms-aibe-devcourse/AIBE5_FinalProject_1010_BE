package com.studyflow.domain.teacher.exception;

import com.studyflow.global.exception.ErrorCode;

// 관리자 인증(isVerified=false)이 안 된 선생님이 선생님 전용 기능을 사용하려 할 때 발생 (403)
public class TeacherNotVerifiedException extends RuntimeException {

    private final ErrorCode errorCode;

    public TeacherNotVerifiedException() {
        super(ErrorCode.TEACHER_NOT_VERIFIED.getMessage());
        this.errorCode = ErrorCode.TEACHER_NOT_VERIFIED;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
