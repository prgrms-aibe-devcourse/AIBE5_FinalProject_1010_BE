package com.studyflow.domain.subscription.service;

import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.service.CreditService;
import com.studyflow.domain.subscription.dto.SubscriptionProductResponse;
import com.studyflow.domain.subscription.dto.SubscriptionSummaryResponse;
import com.studyflow.domain.subscription.dto.UserSubscriptionResponse;
import com.studyflow.domain.subscription.entity.UserSubscription;
import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.domain.subscription.repository.UserSubscriptionRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final CreditService creditService;

    @Transactional(readOnly = true)
    public SubscriptionSummaryResponse getSummary(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return new SubscriptionSummaryResponse(
                creditService.getBalance(userId),
                products(),
                userSubscriptionRepository.findByUserIdOrderByExpiresAtDesc(userId).stream()
                        .map(subscription -> UserSubscriptionResponse.from(subscription, now))
                        .toList());
    }

    @Transactional
    public UserSubscriptionResponse purchase(Long userId, SubscriptionType type) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = userSubscriptionRepository.findTopByUserIdAndTypeOrderByExpiresAtDesc(userId, type)
                .filter(subscription -> subscription.getExpiresAt().isAfter(now))
                .map(UserSubscription::getExpiresAt)
                .orElse(now);
        LocalDateTime expiresAt = startsAt.plusDays(type.getDurationDays());

        UserSubscription saved = userSubscriptionRepository.save(UserSubscription.create(user, type, startsAt, expiresAt));

        creditService.deduct(userId, type.getPriceMileage(), CreditReason.SUBSCRIPTION_PURCHASE, saved.getId());

        return UserSubscriptionResponse.from(saved, now);
    }

    @Transactional
    public void refund(Long subscriptionId, Long userId) {
        UserSubscription subscription = userSubscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("구독권을 찾을 수 없습니다."));

        if (!subscription.getUser().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 구독권만 환불할 수 있습니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        if (subscription.getRefundedAt() != null) {
            throw new IllegalArgumentException("이미 환불 처리된 구독권입니다.");
        }

        if (!subscription.getStartsAt().isAfter(now)) {
            throw new IllegalArgumentException("이미 사용이 시작된 구독권은 환불할 수 없습니다.");
        }

        subscription.refund(now);
        creditService.charge(userId, subscription.getPriceMileage(), CreditReason.REFUND, subscription.getId());
    }

    @Transactional(readOnly = true)
    public boolean hasActiveSubscription(Long userId, SubscriptionType type) {
        return userSubscriptionRepository.existsByUserIdAndTypeAndExpiresAtGreaterThanEqual(
                userId, type, LocalDateTime.now());
    }

    private List<SubscriptionProductResponse> products() {
        return Arrays.stream(SubscriptionType.values())
                .map(SubscriptionProductResponse::from)
                .toList();
    }
}
