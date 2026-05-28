package com.studyflow.domain.enrollment.enums;

/**
 * 수강(등록) 상태
 *
 * ACTIVE    : 수강 중
 * COMPLETED : 수업 정상 종료
 * EXPELLED  : 선생님에 의해 강제 퇴장
 * CANCELLED : 학생 또는 선생님 합의로 취소
 */
public enum EnrollmentStatus {
    ACTIVE, COMPLETED, EXPELLED, CANCELLED
}
