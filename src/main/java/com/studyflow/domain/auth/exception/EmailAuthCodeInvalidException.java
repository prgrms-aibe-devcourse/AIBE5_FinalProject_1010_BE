package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EmailAuthCodeInvalidException extends RuntimeException {

    private final ErrorCode errorCode;

    public EmailAuthCodeInvalidException() {
        super(ErrorCode.EMAIL_AUTH_CODE_INVALID.getMessage());
        this.errorCode = ErrorCode.EMAIL_AUTH_CODE_INVALID;
    }
}
