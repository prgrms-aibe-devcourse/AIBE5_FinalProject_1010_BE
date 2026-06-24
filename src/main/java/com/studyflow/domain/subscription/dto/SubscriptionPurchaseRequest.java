package com.studyflow.domain.subscription.dto;

import com.studyflow.domain.subscription.enums.SubscriptionType;
import jakarta.validation.constraints.NotNull;

public record SubscriptionPurchaseRequest(
        @NotNull SubscriptionType type
) {
}
