package com.studyflow.domain.credit.enums;

/**
 * 마일리지 변동 사유. (이력 기록 + 차감/적립 구분용)
 */
public enum CreditReason {
    CHARGE,             // 토스 결제로 충전(+)
    AI_QUESTION,        // AI 질문 사용(-)
    COURSE_OPEN,        // 강의 개설(-)
    ENROLLMENT_PAY,     // 수강신청 수업료 결제(-) — 학생
    ENROLLMENT_INCOME,  // 수강 정산 수익(+) — 선생님(수업료의 90%)
    SUBSCRIPTION_PURCHASE, // 구독권 구매(-)
    REFUND,             // 환불/취소 복원(+)
    WITHDRAWAL          // 마일리지 환급 출금(-)
}
