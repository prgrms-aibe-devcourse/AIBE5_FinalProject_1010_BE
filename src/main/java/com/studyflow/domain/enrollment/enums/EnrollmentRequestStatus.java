package com.studyflow.domain.enrollment.enums;

/**
 * 수강 신청 상태
 *
 * PENDING   : 신청 완료, 선생님 응답 대기 중
 * ACCEPTED  : 선생님이 수락 → Enrollment 생성됨
 * REJECTED  : 선생님이 거절
 * CANCELLED : 학생이 직접 신청 취소
 */
public enum EnrollmentRequestStatus {
    PENDING, ACCEPTED, REJECTED, CANCELLED
}
