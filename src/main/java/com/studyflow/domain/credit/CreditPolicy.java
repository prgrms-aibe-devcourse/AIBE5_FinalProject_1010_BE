package com.studyflow.domain.credit;

/**
 * 마일리지 정책(단가) 상수. 한 곳에서 관리해 기능별 차감액을 명확히 한다.
 *
 * <p>충전은 1원 = 1마일리지(결제 금액과 동일하게 적립). 기능 사용 시 아래 금액만큼 차감한다.</p>
 */
public final class CreditPolicy {

    /** AI 질문 1회 사용 차감 마일리지. */
    public static final long AI_QUESTION_COST = 100;

    /** 강의 개설 1건 차감 마일리지. */
    public static final long COURSE_OPEN_COST = 1000;

    /** 수강 결제 시 플랫폼이 가져가는 수수료율(%). 나머지(90%)는 선생님에게 마일리지로 적립. */
    public static final long PLATFORM_FEE_PERCENT = 10;

    /** 수업료(price) 중 선생님에게 적립할 금액(수수료 차감 후). 1원 미만은 버림. */
    public static long teacherIncome(long price) {
        return price - (price * PLATFORM_FEE_PERCENT / 100);
    }

    private CreditPolicy() {}
}
