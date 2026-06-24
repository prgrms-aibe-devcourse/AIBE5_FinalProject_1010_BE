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
        boolean active,
        String status
) {
    public static UserSubscriptionResponse from(UserSubscription subscription, LocalDateTime now) {
        String status;
        if (subscription.getRefundedAt() != null) {
            status = "REFUNDED";
        } else if (subscription.getStartsAt().isAfter(now)) {
            status = "SCHEDULED";
        } else if (subscription.getExpiresAt().isBefore(now)) {
            status = "EXPIRED";
        } else {
            status = "ACTIVE";
        }

        return new UserSubscriptionResponse(
                subscription.getId(),
                subscription.getType(),
                subscription.getType().getDisplayName(),
                subscription.getPriceMileage(),
                subscription.getStartsAt(),
                subscription.getExpiresAt(),
                subscription.isActive(now),
                status);
    }
}
