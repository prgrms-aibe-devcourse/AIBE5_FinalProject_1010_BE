package com.studyflow.domain.credit.entity;

import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 마일리지 변동 이력 한 건. (감사/내역 화면용)
 */
@Entity
@Table(name = "credit_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditHistory extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    /** 변동량(+적립 / -차감). */
    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CreditReason reason;

    /** 연관 식별자(결제주문 id / AI질문 id / 강의 id 등). 사유에 따라 의미가 다르다. */
    private Long refId;

    /** 변동 직후 잔액(스냅샷). */
    @Column(nullable = false)
    private long balanceAfter;

    private CreditHistory(Long userId, long amount, CreditReason reason, Long refId, long balanceAfter) {
        this.userId = userId;
        this.amount = amount;
        this.reason = reason;
        this.refId = refId;
        this.balanceAfter = balanceAfter;
    }

    public static CreditHistory of(Long userId, long amount, CreditReason reason, Long refId, long balanceAfter) {
        return new CreditHistory(userId, amount, reason, refId, balanceAfter);
    }
}
