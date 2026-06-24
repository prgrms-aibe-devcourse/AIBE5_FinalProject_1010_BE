package com.studyflow.domain.classroom.exception;

import com.studyflow.global.exception.ErrorCode;

/**
 * 수업당 미리보기 허용 횟수(2회)를 초과한 경우 → 429.
 */
public class PreviewLimitExceededException extends RuntimeException {
    private final ErrorCode errorCode;

    public PreviewLimitExceededException(String message) {
        super(message);
        this.errorCode = ErrorCode.PREVIEW_LIMIT_EXCEEDED;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
