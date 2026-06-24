package com.studyflow.domain.subscription.enums;

public enum SubscriptionType {
    AI_QUESTION("AI 질문 구독권", 5_000L, 30),
    LIVE_CLASS("Live 강의 구독권", 10_000L, 30);

    private final String displayName;
    private final long priceMileage;
    private final int durationDays;

    SubscriptionType(String displayName, long priceMileage, int durationDays) {
        this.displayName = displayName;
        this.priceMileage = priceMileage;
        this.durationDays = durationDays;
    }

    public String getDisplayName() {
        return displayName;
    }

    public long getPriceMileage() {
        return priceMileage;
    }

    public int getDurationDays() {
        return durationDays;
    }
}
