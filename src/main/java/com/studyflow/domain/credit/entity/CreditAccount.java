package com.studyflow.domain.credit.entity;

import com.studyflow.domain.credit.exception.InsufficientCreditException;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사용자별 마일리지 잔액 계정. 사용자당 1개.
 *
 * <p>충전(charge)·차감(deduct)을 엔티티 메서드로 캡슐화한다. 동시 충전/차감의 lost update를
 * 막기 위해 낙관적 락(@Version)을 둔다(같은 트랜잭션 내 read-modify-write 보호).</p>
 */
@Entity
@Table(name = "credit_account")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CreditAccount extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false)
    private long balance = 0;


    private CreditAccount(Long userId) {
        this.userId = userId;
        this.balance = 0;
    }

    public static CreditAccount createFor(Long userId) {
        return new CreditAccount(userId);
    }

    /** 마일리지 적립(충전·환불). 적립 후 잔액 반환. */
    public long charge(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        this.balance += amount;
        return this.balance;
    }

    /** 마일리지 차감. 잔액 부족이면 예외. 차감 후 잔액 반환. */
    public long deduct(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        if (this.balance < amount) {
            throw new InsufficientCreditException(amount, this.balance);
        }
        this.balance -= amount;
        return this.balance;
    }
}
