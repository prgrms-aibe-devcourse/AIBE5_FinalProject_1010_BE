package com.studyflow.domain.payment.enums;

/**
 * 결제 주문 상태.
 */
public enum PaymentStatus {
    READY,    // 주문 생성(결제창 띄우기 전/중)
    DONE,     // 토스 승인 완료
    CANCELED, // 취소/환불
    FAILED    // 승인 실패
}
