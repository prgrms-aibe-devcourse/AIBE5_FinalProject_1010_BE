package com.studyflow.domain.credit.enums;

/**
 * 크레딧 변동 사유. (이력 기록 + 차감/적립 구분용)
 */
public enum CreditReason {
    CHARGE,        // 토스 결제로 충전(+)
    AI_QUESTION,   // AI 질문 사용(-)
    COURSE_OPEN,   // 강의 개설(-)
    REFUND         // 환불/취소 복원(+)
}
