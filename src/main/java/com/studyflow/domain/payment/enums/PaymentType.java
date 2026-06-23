package com.studyflow.domain.payment.enums;

/**
 * 결제 용도. 승인 성공 후 후처리 분기에 쓴다.
 */
public enum PaymentType {
    ENROLLMENT,     // 수강신청 결제(수업료) → 결제 성공 시 수강 등록
    SUBSCRIPTION    // 구독(이용권) 결제 → 결제 성공 시 30일 이용권 부여(AI 질문/강의 개설)
}
