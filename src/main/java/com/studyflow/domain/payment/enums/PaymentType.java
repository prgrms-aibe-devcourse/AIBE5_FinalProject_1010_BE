package com.studyflow.domain.payment.enums;

/**
 * 결제 용도. 현재 토스 결제는 크레딧 충전 한 가지다(수강·AI·강의개설은 크레딧 차감으로 처리).
 */
public enum PaymentType {
    CREDIT_CHARGE   // 크레딧 충전 → 결제 성공 시 크레딧 적립
}
