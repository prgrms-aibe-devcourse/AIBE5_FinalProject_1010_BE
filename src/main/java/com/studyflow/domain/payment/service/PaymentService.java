package com.studyflow.domain.payment.service;

import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.service.CreditService;
import com.studyflow.domain.payment.client.TossPaymentsClient;
import com.studyflow.domain.payment.dto.PaymentDtos.ConfirmRequest;
import com.studyflow.domain.payment.dto.PaymentDtos.ConfirmResponse;
import com.studyflow.domain.payment.dto.PaymentDtos.CreateOrderRequest;
import com.studyflow.domain.payment.dto.PaymentDtos.CreateOrderResponse;
import com.studyflow.domain.payment.entity.PaymentOrder;
import com.studyflow.domain.payment.enums.PaymentType;
import com.studyflow.domain.payment.exception.PaymentException;
import com.studyflow.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 결제 서비스 — 토스 결제로 <b>크레딧을 충전</b>하는 것만 담당한다.
 *
 * <p>크레딧이 플랫폼 화폐다. 충전(현금→크레딧)만 토스를 거치고, 그 크레딧으로
 * AI 질문·강의 개설·수강신청을 결제한다(각 도메인 서비스에서 차감). 즉 토스 결제는
 * 오직 충전 한 가지 용도다.</p>
 *
 * <p>보안 원칙: 결제 금액은 <b>서버가 정한 주문 금액</b>이 기준이다. 승인 시 프론트가 보낸 금액이
 * 주문 금액과 일치하는지 확인하고, 토스 승인 API(시크릿 키)로 한 번 더 검증한다.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentOrderRepository paymentOrderRepository;
    private final TossPaymentsClient tossPaymentsClient;
    private final CreditService creditService;

    /** 충전 결제창을 열기 전 주문을 생성한다(충전 금액 = 적립 크레딧, 1원=1크레딧). */
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest req) {
        if (req.amount() <= 0) throw new PaymentException("충전 금액이 올바르지 않습니다.");

        String orderId = "ord_" + UUID.randomUUID().toString().replace("-", "");
        long amount = req.amount();
        String orderName = "크레딧 " + amount + " 충전";

        // refId = 적립할 크레딧 수(1원=1크레딧)
        PaymentOrder order = PaymentOrder.create(orderId, userId, orderName, amount, PaymentType.CREDIT_CHARGE, amount);
        paymentOrderRepository.save(order);
        return new CreateOrderResponse(orderId, orderName, amount);
    }

    /**
     * 결제 승인. 주문 검증 → 토스 승인 → 크레딧 적립.
     * 이미 처리된 주문이면 중복 적립 없이 현재 잔액을 그대로 반환(멱등).
     */
    @Transactional
    public ConfirmResponse confirm(Long userId, ConfirmRequest req) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> new PaymentException("주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new PaymentException("본인 주문만 결제할 수 있습니다.");
        }
        if (order.isDone()) {
            // 멱등: 이미 승인된 주문은 재적립하지 않는다.
            return new ConfirmResponse(order.getOrderId(), order.getAmount(), creditService.getBalance(userId));
        }
        // ★ 금액 위변조 방지: 프론트가 보낸 금액 == 우리가 정한 주문 금액
        if (order.getAmount() != req.amount()) {
            order.markFailed();
            throw new PaymentException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 토스 승인(시크릿 키). 실패 시 예외 → 트랜잭션 롤백.
        tossPaymentsClient.confirm(req.paymentKey(), req.orderId(), req.amount());
        order.markDone(req.paymentKey());

        long creditBalance = creditService.charge(userId, order.getRefId(), CreditReason.CHARGE, order.getId());
        return new ConfirmResponse(order.getOrderId(), order.getAmount(), creditBalance);
    }
}
