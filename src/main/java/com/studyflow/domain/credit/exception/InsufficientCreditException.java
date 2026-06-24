package com.studyflow.domain.credit.exception;

/**
 * 마일리지 잔액 부족. 기능 사용(AI 질문/강의 개설) 시 잔액이 모자라면 발생.
 * 프론트는 이 에러(code=INSUFFICIENT_CREDIT)를 받으면 충전 안내로 유도한다.
 */
public class InsufficientCreditException extends RuntimeException {
    public InsufficientCreditException(long required, long balance) {
        super("마일리지가 부족합니다. (필요 " + required + " / 보유 " + balance + ")");
    }
}
