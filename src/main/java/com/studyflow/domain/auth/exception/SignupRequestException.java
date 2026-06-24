package com.studyflow.domain.auth.exception;

import com.studyflow.global.exception.ErrorCode;

/**
 * 회원가입 요청(SignupRequest) 처리 중 발생하는 입력값 관련 예외를 통합한 커스텀 런타임 예외입니다.
 *
 * 기존에 분리되어 있던 InvalidBirthDateException, InvalidGenderException, InvalidRoleException
 * 등을 하나로 합쳐서 컨트롤러 및 예외 처리기를 단순화하기 위해 사용합니다.
 */
public class SignupRequestException extends RuntimeException {
    private final ErrorCode errorCode;

    public SignupRequestException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
