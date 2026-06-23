package com.studyflow.domain.payment.service;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.service.CreditService;
import com.studyflow.domain.enrollment.service.EnrollmentRequestService;
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
 * 결제 서비스 — 주문 생성 + 토스 승인 검증 + 후처리(크레딧 적립/수강 등록).
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
    private final CourseRepository courseRepository;
    private final EnrollmentRequestService enrollmentRequestService;

    /** 결제창을 열기 전 주문을 생성한다(금액·용도를 서버가 확정). */
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest req) {
        String orderId = "ord_" + UUID.randomUUID().toString().replace("-", "");

        long amount;
        String orderName;
        Long refId;

        if (req.type() == PaymentType.CREDIT_CHARGE) {
            if (req.amount() <= 0) throw new PaymentException("충전 금액이 올바르지 않습니다.");
            amount = req.amount();
            orderName = "크레딧 " + amount + " 충전";
            refId = amount; // 적립할 크레딧 수(1원=1크레딧)
        } else { // ENROLLMENT
            if (req.refId() == null) throw new PaymentException("수강할 강의 정보가 없습니다.");
            Course course = courseRepository.findById(req.refId())
                    .orElseThrow(() -> new PaymentException("강의를 찾을 수 없습니다."));
            amount = course.getPricePerSession();
            if (amount <= 0) throw new PaymentException("결제할 수업료가 올바르지 않습니다.");
            orderName = "수강 결제 - " + course.getTitle();
            refId = course.getId();
        }

        PaymentOrder order = PaymentOrder.create(orderId, userId, orderName, amount, req.type(), refId);
        paymentOrderRepository.save(order);
        return new CreateOrderResponse(orderId, orderName, amount);
    }

    /**
     * 결제 승인. 주문 검증 → 토스 승인 → 후처리(크레딧 적립/수강 등록).
     * 이미 처리된 주문이면 중복 적립 없이 현재 상태를 그대로 반환(멱등).
     */
    @Transactional
    public ConfirmResponse confirm(Long userId, ConfirmRequest req) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> new PaymentException("주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new PaymentException("본인 주문만 결제할 수 있습니다.");
        }
        if (order.isDone()) {
            // 멱등: 이미 승인된 주문은 재처리하지 않는다(중복 적립/중복 등록 방지).
            return new ConfirmResponse(order.getOrderId(), order.getType(), order.getAmount(),
                    creditService.getBalance(userId),
                    order.getType() == PaymentType.ENROLLMENT ? order.getRefId() : null);
        }
        // ★ 금액 위변조 방지: 프론트가 보낸 금액 == 우리가 정한 주문 금액
        if (order.getAmount() != req.amount()) {
            order.markFailed();
            throw new PaymentException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 토스 승인(시크릿 키). 실패 시 예외 → 트랜잭션 롤백.
        tossPaymentsClient.confirm(req.paymentKey(), req.orderId(), req.amount());
        order.markDone(req.paymentKey());

        long creditBalance = creditService.getBalance(userId);
        Long enrolledCourseId = null;

        if (order.getType() == PaymentType.CREDIT_CHARGE) {
            creditBalance = creditService.charge(userId, order.getRefId(), CreditReason.CHARGE, order.getId());
        } else { // ENROLLMENT
            enrolledCourseId = enrollmentRequestService.enrollByPayment(order.getRefId(), userId);
        }

        return new ConfirmResponse(order.getOrderId(), order.getType(), order.getAmount(), creditBalance, enrolledCourseId);
    }
}
