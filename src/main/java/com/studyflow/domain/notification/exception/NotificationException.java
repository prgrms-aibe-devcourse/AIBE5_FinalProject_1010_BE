package com.studyflow.domain.notification.exception;

import com.studyflow.global.exception.ErrorCode;

// 알림 조회/읽음 처리 과정에서 발생하는 예외 (NOT_FOUND, ACCESS_FORBIDDEN 등)
public class NotificationException extends RuntimeException {
    private final ErrorCode errorCode;

    public NotificationException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
