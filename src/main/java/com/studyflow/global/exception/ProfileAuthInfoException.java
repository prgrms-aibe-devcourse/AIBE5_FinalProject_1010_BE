package com.studyflow.global.exception;

public class ProfileAuthInfoException extends RuntimeException {
    private final ErrorCode errorCode;

    public ProfileAuthInfoException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
