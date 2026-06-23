package com.studyflow.domain.payment.service;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.repository.CourseRepository;
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
import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 서비스 — 주문 생성 + 토스 승인 검증 + 후처리(구독 부여/수강 등록).
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
    private final SubscriptionService subscriptionService;
    private final CourseRepository courseRepository;
    private final EnrollmentRequestService enrollmentRequestService;

    /** 결제창을 열기 전 주문을 생성한다(금액·용도를 서버가 확정). */
    @Transactional
    public CreateOrderResponse createOrder(Long userId, CreateOrderRequest req) {
        String orderId = "ord_" + UUID.randomUUID().toString().replace("-", "");
        PaymentOrder order;

        if (req.type() == PaymentType.SUBSCRIPTION) {
            SubscriptionType subType = req.subscriptionType();
            if (subType == null) throw new PaymentException("구독 종류가 없습니다.");
            String orderName = subType.getDisplayName();
            order = PaymentOrder.forSubscription(orderId, userId, orderName, subType.getPrice(), subType);
        } else { // ENROLLMENT
            if (req.refId() == null) throw new PaymentException("수강할 강의 정보가 없습니다.");
            Course course = courseRepository.findById(req.refId())
                    .orElseThrow(() -> new PaymentException("강의를 찾을 수 없습니다."));
            long amount = course.getPricePerSession();
            if (amount <= 0) throw new PaymentException("결제할 수업료가 올바르지 않습니다.");
            order = PaymentOrder.forEnrollment(orderId, userId, "수강 결제 - " + course.getTitle(), amount, course.getId());
        }

        paymentOrderRepository.save(order);
        return new CreateOrderResponse(order.getOrderId(), order.getOrderName(), order.getAmount());
    }

    /**
     * 결제 승인. 주문 검증 → 토스 승인 → 후처리(구독 부여/수강 등록).
     * 이미 처리된 주문이면 중복 처리 없이 현재 상태를 그대로 반환(멱등).
     */
    @Transactional
    public ConfirmResponse confirm(Long userId, ConfirmRequest req) {
        PaymentOrder order = paymentOrderRepository.findByOrderId(req.orderId())
                .orElseThrow(() -> new PaymentException("주문을 찾을 수 없습니다."));

        if (!order.getUserId().equals(userId)) {
            throw new PaymentException("본인 주문만 결제할 수 있습니다.");
        }
        if (order.isDone()) {
            // 멱등: 이미 승인된 주문은 재처리하지 않는다(중복 부여/중복 등록 방지).
            return new ConfirmResponse(order.getOrderId(), order.getType(), order.getAmount(),
                    order.getType() == PaymentType.ENROLLMENT ? order.getRefId() : null,
                    order.getSubType(), null);
        }
        // ★ 금액 위변조 방지: 프론트가 보낸 금액 == 우리가 정한 주문 금액
        if (order.getAmount() != req.amount()) {
            order.markFailed();
            throw new PaymentException("결제 금액이 주문 금액과 일치하지 않습니다.");
        }

        // 토스 승인(시크릿 키). 실패 시 예외 → 트랜잭션 롤백.
        tossPaymentsClient.confirm(req.paymentKey(), req.orderId(), req.amount());
        order.markDone(req.paymentKey());

        Long enrolledCourseId = null;
        LocalDateTime subExpiresAt = null;

        if (order.getType() == PaymentType.SUBSCRIPTION) {
            subExpiresAt = subscriptionService.grant(userId, order.getSubType());
        } else { // ENROLLMENT
            enrolledCourseId = enrollmentRequestService.enrollByPayment(order.getRefId(), userId);
        }

        return new ConfirmResponse(order.getOrderId(), order.getType(), order.getAmount(),
                enrolledCourseId, order.getSubType(), subExpiresAt);
    }
}
