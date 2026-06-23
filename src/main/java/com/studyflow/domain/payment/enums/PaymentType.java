package com.studyflow.domain.payment.enums;

/**
 * 결제 용도. 현재 토스 결제는 마일리지 충전 한 가지다(수강·AI·강의개설은 마일리지 차감으로 처리).
 */
public enum PaymentType {
    CREDIT_CHARGE   // 마일리지 충전 → 결제 성공 시 마일리지 적립
}
