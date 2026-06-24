package com.studyflow.domain.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 결제(마일리지 충전) 관련 요청/응답 DTO 모음.
 */
public final class PaymentDtos {

    private PaymentDtos() {}

    /**
     * 충전 주문 생성 요청. 결제창을 열기 전 서버에 주문을 만든다(금액 서버 확정).
     * amount = 충전 금액(원). 적립 마일리지도 동일(1원=1마일리지).
     */
    public record CreateOrderRequest(
            @Min(1) long amount
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

    /** 결제 승인 결과(충전 후 잔액 안내). */
    public record ConfirmResponse(
            String orderId,
            long amount,
            long mileageBalance   // 충전 적립 후 잔액
    ) {}
}
