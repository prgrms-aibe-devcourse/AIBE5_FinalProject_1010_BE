package com.studyflow.domain.subscription.dto;

import com.studyflow.domain.subscription.entity.UserSubscription;
import com.studyflow.domain.subscription.enums.SubscriptionType;

import java.time.LocalDateTime;

public record SubscriptionProductResponse(
        SubscriptionType type,
        String name,
        long priceMileage,
        int durationDays
) {
    public static SubscriptionProductResponse from(SubscriptionType type) {
        return new SubscriptionProductResponse(
                type,
                type.getDisplayName(),
                type.getPriceMileage(),
                type.getDurationDays());
    }
}
