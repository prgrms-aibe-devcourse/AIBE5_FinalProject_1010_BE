package com.studyflow.domain.auth.exception;

/**
 * 회원가입 요청에서 birthDate 파싱이 실패했을 때 던져지는 예외
 */
public class InvalidBirthDateException extends RuntimeException {
    public InvalidBirthDateException(String message) {
        super(message);
    }

    public InvalidBirthDateException(String message, Throwable cause) {
        super(message, cause);
    }
}

