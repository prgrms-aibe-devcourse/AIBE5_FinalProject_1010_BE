package com.studyflow.domain.admin.dto;

import com.studyflow.domain.credit.enums.CreditReason;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
public class AdminCreditHistoryResponse {
    private Long id;
    private Long userId;
    private String email;
    private String name;
    private long amount;
    private CreditReason reason;
    private Long refId;
    private long balanceAfter;
    private LocalDateTime createdAt;

    @Setter
    private String detail; // 구독권 이름이나 수업명 등 상세 내용 추가

    public AdminCreditHistoryResponse(Long id, Long userId, String email, String name, long amount, CreditReason reason, Long refId, long balanceAfter, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.amount = amount;
        this.reason = reason;
        this.refId = refId;
        this.balanceAfter = balanceAfter;
        this.createdAt = createdAt;
    }
}
