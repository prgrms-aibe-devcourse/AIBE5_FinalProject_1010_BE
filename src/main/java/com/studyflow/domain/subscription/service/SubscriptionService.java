package com.studyflow.domain.subscription.service;

import com.studyflow.domain.subscription.entity.Subscription;
import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.domain.subscription.exception.SubscriptionRequiredException;
import com.studyflow.domain.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 구독(이용권) 도메인 서비스. 결제로 부여, 기능 사용 전 활성 검사.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Transactional(readOnly = true)
    public boolean hasActive(Long userId, SubscriptionType type) {
        return subscriptionRepository.findByUserIdAndType(userId, type)
                .map(s -> s.isActive(LocalDateTime.now()))
                .orElse(false);
    }

    /** 활성 구독이 없으면 예외(기능 사용 차단 → 구독 유도). */
    @Transactional(readOnly = true)
    public void requireActive(Long userId, SubscriptionType type) {
        if (!hasActive(userId, type)) {
            throw new SubscriptionRequiredException(type);
        }
    }

    /** 결제 성공 시 호출 — 30일 이용권 부여(활성 중이면 이어서 연장). 만료일 반환. */
    @Transactional
    public LocalDateTime grant(Long userId, SubscriptionType type) {
        LocalDateTime now = LocalDateTime.now();
        Subscription sub = subscriptionRepository.findByUserIdAndType(userId, type)
                .orElseGet(() -> subscriptionRepository.save(
                        Subscription.createFor(userId, type, now))); // 신규는 now로 시작(아래 extend가 30일 더함)
        sub.extend(now, SubscriptionType.DURATION_DAYS);
        return sub.getExpiresAt();
    }

    /** FE용: 내 구독 목록(타입·만료일·활성여부). */
    @Transactional(readOnly = true)
    public List<Subscription> getMySubscriptions(Long userId) {
        return subscriptionRepository.findByUserId(userId);
    }
}
