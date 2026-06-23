package com.studyflow.domain.subscription.repository;

import com.studyflow.domain.subscription.entity.Subscription;
import com.studyflow.domain.subscription.enums.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserIdAndType(Long userId, SubscriptionType type);
    List<Subscription> findByUserId(Long userId);
}
