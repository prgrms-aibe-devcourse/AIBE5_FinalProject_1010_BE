package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EmailRateLimitException extends RuntimeException {

    private final ErrorCode errorCode;

    public EmailRateLimitException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
