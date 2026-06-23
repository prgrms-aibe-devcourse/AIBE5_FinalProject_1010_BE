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

        creditService.deduct(userId, type.getPriceMileage(), CreditReason.SUBSCRIPTION_PURCHASE, null);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startsAt = userSubscriptionRepository.findTopByUserIdAndTypeOrderByExpiresAtDesc(userId, type)
                .filter(subscription -> subscription.getExpiresAt().isAfter(now))
                .map(UserSubscription::getExpiresAt)
                .orElse(now);
        LocalDateTime expiresAt = startsAt.plusDays(type.getDurationDays());

        UserSubscription saved = userSubscriptionRepository.save(UserSubscription.create(user, type, startsAt, expiresAt));
        return UserSubscriptionResponse.from(saved, now);
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
