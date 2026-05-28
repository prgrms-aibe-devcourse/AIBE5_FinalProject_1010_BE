package com.studyflow.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token.");

    private final HttpStatus status;
    private final String message;
}

