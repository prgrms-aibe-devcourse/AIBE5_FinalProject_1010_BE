package com.studyflow.domain.classroom.exception;

import com.studyflow.global.exception.ErrorCode;

/**
 * 강의실 권한 위배 → 403.
 *
 * <p>예: 담당 선생님이 아닌 사용자의 강의실 열기/종료/권한변경,
 * 수강생/담당교사가 아닌 사용자의 강의실 조회·참가 등.</p>
 */
public class ClassroomForbiddenException extends RuntimeException {
    private final ErrorCode errorCode;

    public ClassroomForbiddenException(String message) {
        super(message);
        this.errorCode = ErrorCode.CLASSROOM_FORBIDDEN;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
