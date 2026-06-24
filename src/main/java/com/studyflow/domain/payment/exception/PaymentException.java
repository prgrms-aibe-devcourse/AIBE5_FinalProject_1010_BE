package com.studyflow.domain.payment.exception;

/**
 * 결제 처리 실패(승인 실패·금액 불일치·주문 없음·중복 등).
 */
public class PaymentException extends RuntimeException {
    public PaymentException(String message) {
        super(message);
    }
}
