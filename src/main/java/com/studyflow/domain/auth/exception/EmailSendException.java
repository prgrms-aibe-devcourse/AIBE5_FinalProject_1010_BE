package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;
import lombok.Getter;

@Getter
public class EmailSendException extends RuntimeException {

    private final ErrorCode errorCode;

    public EmailSendException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
