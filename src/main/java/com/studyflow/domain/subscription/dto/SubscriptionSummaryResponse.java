package com.studyflow.domain.subscription.dto;

import java.util.List;

public record SubscriptionSummaryResponse(
        long mileageBalance,
        List<SubscriptionProductResponse> products,
        List<UserSubscriptionResponse> subscriptions
) {
}
