package com.studyflow.domain.credit.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.studyflow.domain.credit.enums.WithdrawalStatus;
import java.time.LocalDateTime;

@Entity
@Table(name = "withdrawal_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class WithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String accountNumber;

    @Column(nullable = false)
    private String accountHolder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WithdrawalStatus status;

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public static WithdrawalRequest create(Long userId, long amount, String bankName, String accountNumber, String accountHolder) {
        WithdrawalRequest wr = new WithdrawalRequest();
        wr.userId = userId;
        wr.amount = amount;
        wr.bankName = bankName;
        wr.accountNumber = accountNumber;
        wr.accountHolder = accountHolder;
        wr.status = WithdrawalStatus.PENDING;
        return wr;
    }

    public void approve() {
        this.status = WithdrawalStatus.APPROVED;
    }

    public void reject() {
        this.status = WithdrawalStatus.REJECTED;
    }
}
