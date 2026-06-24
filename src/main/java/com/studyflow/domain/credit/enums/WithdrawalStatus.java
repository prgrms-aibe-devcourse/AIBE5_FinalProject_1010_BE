package com.studyflow.domain.credit.enums;

public enum WithdrawalStatus {
    PENDING,   // 환급 대기 (심사 중)
    APPROVED,  // 환급 승인 (현금 지급 완료)
    REJECTED   // 환급 거절 (마일리지 복원)
}
