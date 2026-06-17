package com.studyflow.domain.notification.enums;

// 알림 종류 — FE는 type + relatedId 조합으로 클릭 시 이동할 경로를 결정함
public enum NotificationType {
    ENROLLMENT_REQUESTED,  // 학생이 수강 신청 → 선생님 수신
    ENROLLMENT_ACCEPTED,   // 선생님이 신청 수락 → 학생 수신
    ENROLLMENT_REJECTED,   // 선생님이 신청 거절 → 학생 수신
    ENROLLMENT_CANCELLED,  // 학생이 신청 취소 → 선생님 수신
    QNA_ANSWERED,          // 선생님이 답변 작성 → 질문 작성 학생 수신 (relatedId = questionId)
    CLASSROOM_OPENED       // 선생님이 강의실 열기 → ACTIVE 수강생 수신 (relatedId = courseId)
}
