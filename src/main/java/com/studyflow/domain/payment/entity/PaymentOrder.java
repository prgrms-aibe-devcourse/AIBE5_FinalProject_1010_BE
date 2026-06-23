package com.studyflow.domain.payment.entity;

import com.studyflow.domain.payment.enums.PaymentStatus;
import com.studyflow.domain.payment.enums.PaymentType;
import com.studyflow.domain.subscription.enums.SubscriptionType;
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

import java.time.LocalDateTime;

/**
 * 결제 주문 한 건. 토스 결제창을 열기 전 우리가 먼저 생성하고(READY),
 * 승인 시 금액 위변조 검증의 기준이 된다(주문 금액 == 결제 금액).
 *
 * <p>orderId는 토스에 보낼 우리 주문번호(고유). paymentKey는 토스가 발급한 결제 식별자(승인 시 기록).
 * refId는 용도별 의미: ENROLLMENT=courseId, CREDIT_CHARGE=적립할 크레딧 수.</p>
 */
@Entity
@Table(name = "payment_order")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 우리가 생성한 주문번호(토스 orderId). 고유. */
    @Column(nullable = false, unique = true, length = 64)
    private String orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String orderName;

    @Column(nullable = false)
    private long amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status = PaymentStatus.READY;

    /** 토스가 발급한 결제 식별자(승인 시 기록). */
    @Column(length = 200)
    private String paymentKey;

    /** 용도별 연관값: ENROLLMENT=courseId. (SUBSCRIPTION은 subType 사용) */
    private Long refId;

    /** SUBSCRIPTION 결제일 때 부여할 구독 종류(ENROLLMENT면 null). */
    @Enumerated(EnumType.STRING)
    private SubscriptionType subType;

    private LocalDateTime approvedAt;

    private PaymentOrder(String orderId, Long userId, String orderName, long amount, PaymentType type, Long refId, SubscriptionType subType) {
        this.orderId = orderId;
        this.userId = userId;
        this.orderName = orderName;
        this.amount = amount;
        this.type = type;
        this.refId = refId;
        this.subType = subType;
        this.status = PaymentStatus.READY;
    }

    /** 수강신청(ENROLLMENT) 주문. refId=courseId. */
    public static PaymentOrder forEnrollment(String orderId, Long userId, String orderName, long amount, Long courseId) {
        return new PaymentOrder(orderId, userId, orderName, amount, PaymentType.ENROLLMENT, courseId, null);
    }

    /** 구독(SUBSCRIPTION) 주문. subType=구독 종류. */
    public static PaymentOrder forSubscription(String orderId, Long userId, String orderName, long amount, SubscriptionType subType) {
        return new PaymentOrder(orderId, userId, orderName, amount, PaymentType.SUBSCRIPTION, null, subType);
    }

    public void markDone(String paymentKey) {
        this.paymentKey = paymentKey;
        this.status = PaymentStatus.DONE;
        this.approvedAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = PaymentStatus.FAILED;
    }

    public boolean isDone() {
        return this.status == PaymentStatus.DONE;
    }
}
