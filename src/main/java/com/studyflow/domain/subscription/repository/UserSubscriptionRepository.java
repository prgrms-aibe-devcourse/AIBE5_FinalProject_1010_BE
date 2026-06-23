package com.studyflow.domain.subscription.repository;

import com.studyflow.domain.subscription.entity.UserSubscription;
import com.studyflow.domain.subscription.enums.SubscriptionType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserSubscriptionRepository extends JpaRepository<UserSubscription, Long> {
    List<UserSubscription> findByUserIdOrderByExpiresAtDesc(Long userId);

    Optional<UserSubscription> findTopByUserIdAndTypeOrderByExpiresAtDesc(Long userId, SubscriptionType type);

    boolean existsByUserIdAndTypeAndExpiresAtGreaterThanEqual(Long userId, SubscriptionType type, LocalDateTime now);
}
