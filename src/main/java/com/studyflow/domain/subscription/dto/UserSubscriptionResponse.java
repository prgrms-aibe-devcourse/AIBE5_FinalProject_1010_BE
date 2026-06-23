package com.studyflow.domain.subscription.dto;

import com.studyflow.domain.subscription.entity.UserSubscription;
import com.studyflow.domain.subscription.enums.SubscriptionType;

import java.time.LocalDateTime;

public record UserSubscriptionResponse(
        Long id,
        SubscriptionType type,
        String name,
        long priceMileage,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        boolean active
) {
    public static UserSubscriptionResponse from(UserSubscription subscription, LocalDateTime now) {
        return new UserSubscriptionResponse(
                subscription.getId(),
                subscription.getType(),
                subscription.getType().getDisplayName(),
                subscription.getPriceMileage(),
                subscription.getStartsAt(),
                subscription.getExpiresAt(),
                subscription.isActive(now));
    }
}
