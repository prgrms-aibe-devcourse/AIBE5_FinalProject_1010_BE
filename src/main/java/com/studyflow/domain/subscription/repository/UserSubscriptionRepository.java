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

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(u) > 0 FROM UserSubscription u WHERE u.user.id = :userId AND u.type = :type AND u.refundedAt IS NULL AND u.startsAt <= :now AND u.expiresAt >= :now")
    boolean hasActiveSubscription(@org.springframework.data.repository.query.Param("userId") Long userId, @org.springframework.data.repository.query.Param("type") SubscriptionType type, @org.springframework.data.repository.query.Param("now") LocalDateTime now);
}
