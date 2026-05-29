package com.studyflow.domain.auth.exception;

/**
 * 회원 가입 또는 권한 처리 중 전달된 role 값이 시스템에서 정의된 UserRole enum으로 변환될 수 없을 때
 * 발생시키는 런타임 예외입니다.
 *
 * 예: 요청에 들어온 role이 "STUDENT", "TEACHER", "ADMIN" 중 하나가 아닌 경우
 */
public class InvalidRoleException extends RuntimeException {
    /**
     * 예외 생성자
     * @param message 상세 예외 메시지
     */
    public InvalidRoleException(String message) {
        super(message);
    }
}

