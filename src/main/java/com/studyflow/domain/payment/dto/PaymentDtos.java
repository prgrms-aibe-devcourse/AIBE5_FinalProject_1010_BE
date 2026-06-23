package com.studyflow.domain.payment.dto;

import com.studyflow.domain.payment.enums.PaymentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 결제 관련 요청/응답 DTO 모음.
 */
public final class PaymentDtos {

    private PaymentDtos() {}

    /**
     * 주문 생성 요청. 결제창을 열기 전 서버에 주문을 만든다(금액·용도 서버 확정).
     * - CREDIT_CHARGE: amount = 충전 금액(원). 적립 크레딧도 동일(1원=1크레딧).
     * - ENROLLMENT: refId = courseId. 금액은 서버가 수업료로 계산하므로 amount 무시.
     */
    public record CreateOrderRequest(
            @NotNull PaymentType type,
            @Min(0) long amount,
            Long refId
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
            long creditBalance,   // 충전이면 적립 후 잔액(아니면 현재 잔액)
            Long enrolledCourseId // 수강결제면 등록된 courseId
    ) {}
}
