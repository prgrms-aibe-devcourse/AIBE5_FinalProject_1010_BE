package com.studyflow.domain.payment.service;

import com.studyflow.domain.payment.entity.PaymentOrder;
import com.studyflow.domain.payment.repository.PaymentOrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOrderSupport {

    private final PaymentOrderRepository paymentOrderRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedInNewTransaction(Long id) {
        paymentOrderRepository.findById(id).ifPresent(PaymentOrder::markFailed);
    }
}
