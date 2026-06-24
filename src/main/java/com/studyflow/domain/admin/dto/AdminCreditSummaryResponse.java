package com.studyflow.domain.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminCreditSummaryResponse {
    private long totalCharge; // 마일리지 충전
    private long totalIncome; // 수업 수익
    private long totalSpent;  // 서비스 구매/사용 내역 (차감액)
    private long totalRefund; // 환불 및 취소 복원
}
