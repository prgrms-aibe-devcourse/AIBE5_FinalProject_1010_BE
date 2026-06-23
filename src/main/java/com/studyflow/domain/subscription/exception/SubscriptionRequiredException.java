package com.studyflow.domain.subscription.exception;

import com.studyflow.domain.subscription.enums.SubscriptionType;

/**
 * 활성 구독(이용권)이 없을 때. 기능 사용(AI 질문/강의 개설) 전 검사에서 발생.
 * 프론트는 code=SUBSCRIPTION_REQUIRED(402)를 받으면 구독 안내로 유도한다.
 */
public class SubscriptionRequiredException extends RuntimeException {
    public SubscriptionRequiredException(SubscriptionType type) {
        super(type.getDisplayName() + "이 필요합니다. 구독 후 이용해 주세요.");
    }
}
