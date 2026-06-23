package com.studyflow.domain.payment.dto;

import com.studyflow.domain.payment.enums.PaymentType;
import com.studyflow.domain.subscription.enums.SubscriptionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

import java.time.LocalDateTime;

/**
 * 결제 관련 요청/응답 DTO 모음.
 */
public final class PaymentDtos {

    private PaymentDtos() {}

    /**
     * 주문 생성 요청. 결제창을 열기 전 서버에 주문을 만든다(금액·용도 서버 확정).
     * - SUBSCRIPTION: subscriptionType 필수. 금액은 서버가 요금제에서 결정.
     * - ENROLLMENT: refId = courseId 필수. 금액은 서버가 수업료로 결정.
     */
    public record CreateOrderRequest(
            @NotNull PaymentType type,
            Long refId,                          // ENROLLMENT: courseId
            SubscriptionType subscriptionType    // SUBSCRIPTION: 구독 종류
    ) {}

    /** 주문 생성 응답. 프론트는 이 값으로 토스 결제창을 연다. */
    public record CreateOrderResponse(
            String orderId,
            String orderName,
            long amount
    ) {}

    /** 결제 승인 요청(프론트 성공 콜백에서 받은 값 그대로 전달). */
    public record ConfirmRequest(
            @NotBlank String paymentKey,
            @NotBlank String orderId,
            @Min(1) long amount
    ) {}

    /** 결제 승인 결과. */
    public record ConfirmResponse(
            String orderId,
            PaymentType type,
            long amount,
            Long enrolledCourseId,             // 수강결제면 등록된 courseId
            SubscriptionType subscriptionType, // 구독결제면 구독 종류
            LocalDateTime subscriptionExpiresAt // 구독결제면 만료일
    ) {}
}
