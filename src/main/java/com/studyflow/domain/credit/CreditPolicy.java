package com.studyflow.domain.credit;

/**
 * 크레딧 정책(단가) 상수. 한 곳에서 관리해 기능별 차감액을 명확히 한다.
 *
 * <p>충전은 1원 = 1크레딧(결제 금액과 동일하게 적립). 기능 사용 시 아래 금액만큼 차감한다.</p>
 */
public final class CreditPolicy {

    /** AI 질문 1회 사용 차감 크레딧. */
    public static final long AI_QUESTION_COST = 100;

    /** 강의 개설 1건 차감 크레딧. */
    public static final long COURSE_OPEN_COST = 1000;

    private CreditPolicy() {}
}
