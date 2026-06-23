package com.studyflow.domain.subscription.enums;

/**
 * 구독(이용권) 종류. 일회성 결제로 가격만큼 결제하면 30일 이용권이 부여된다.
 *
 * <p>AI 질문/강의 개설처럼 "기간 내 무제한" 모델. 수강신청은 구독이 아니라 일회성 결제(별도).</p>
 */
public enum SubscriptionType {
    AI_QUESTION(5000, "AI 질문 1개월 이용권"),
    COURSE_OPEN(10000, "강의 개설 1개월 이용권");

    /** 1개월 이용권 가격(원). 충전이 아니라 결제 금액 = 이 값. */
    private final long price;
    private final String displayName;

    /** 1회 결제 시 부여되는 이용 기간(일). */
    public static final int DURATION_DAYS = 30;

    SubscriptionType(long price, String displayName) {
        this.price = price;
        this.displayName = displayName;
    }

    public long getPrice() { return price; }
    public String getDisplayName() { return displayName; }
}
