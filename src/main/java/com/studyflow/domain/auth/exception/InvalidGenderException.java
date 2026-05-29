package com.studyflow.domain.auth.exception;

/**
 * 회원 가입 또는 사용자 정보 처리 중 전달된 성별 값이 허용된 enum 값으로 변환될 수 없을 때 던져지는 예외입니다.
 *
 * 예: 요청에 들어온 gender가 "MALE" 또는 "FEMALE" 이외의 값인 경우
 */
public class InvalidGenderException extends RuntimeException {
    /**
     * 예외 생성자
     * @param message 상세 예외 메시지(사용자에게 반환될 메시지 또는 로그용)
     */
    public InvalidGenderException(String message) {
        super(message);
    }
}

