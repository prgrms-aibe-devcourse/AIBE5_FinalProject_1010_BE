package com.studyflow.domain.payment.controller;

import com.studyflow.domain.payment.config.TossPaymentsProperties;
import com.studyflow.domain.payment.dto.PaymentDtos.ConfirmRequest;
import com.studyflow.domain.payment.dto.PaymentDtos.ConfirmResponse;
import com.studyflow.domain.payment.dto.PaymentDtos.CreateOrderRequest;
import com.studyflow.domain.payment.dto.PaymentDtos.CreateOrderResponse;
import com.studyflow.domain.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 결제 API.
 * - POST /api/v1/payments/orders   : 결제창 열기 전 주문 생성(금액·용도 서버 확정)
 * - POST /api/v1/payments/confirm  : 토스 성공 콜백 후 승인 검증 + 후처리
 * - GET  /api/v1/payments/client-key: 프론트가 결제창에 쓸 토스 클라이언트 키
 */
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final TossPaymentsProperties tossProps;

    @PostMapping("/orders")
    public ResponseEntity<CreateOrderResponse> createOrder(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(paymentService.createOrder(userId, request));
    }

    @PostMapping("/confirm")
    public ResponseEntity<ConfirmResponse> confirm(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ConfirmRequest request) {
        return ResponseEntity.ok(paymentService.confirm(userId, request));
    }

    /** 프론트 결제창 초기화용 클라이언트 키(공개값). */
    @GetMapping("/client-key")
    public ResponseEntity<Map<String, String>> clientKey() {
        return ResponseEntity.ok(Map.of("clientKey", tossProps.clientKey()));
    }
}
