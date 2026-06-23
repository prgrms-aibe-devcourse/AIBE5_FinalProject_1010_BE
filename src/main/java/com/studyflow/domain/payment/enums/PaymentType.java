package com.studyflow.domain.payment.enums;

/**
 * 결제 용도. 승인 성공 후 후처리 분기에 쓴다.
 */
public enum PaymentType {
    ENROLLMENT,     // 수강신청 결제(수업료) → 결제 성공 시 수강 등록
    CREDIT_CHARGE   // 크레딧 충전 → 결제 성공 시 크레딧 적립
}
