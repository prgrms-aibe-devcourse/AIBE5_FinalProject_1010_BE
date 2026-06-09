package com.studyflow.domain.admin.exception;

import com.studyflow.global.exception.ErrorCode;

public class StatisticsDateNotPastException extends RuntimeException {

    private final ErrorCode errorCode;

    public StatisticsDateNotPastException() {
        super(ErrorCode.STATISTICS_DATE_NOT_PAST.getMessage());
        this.errorCode = ErrorCode.STATISTICS_DATE_NOT_PAST;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
