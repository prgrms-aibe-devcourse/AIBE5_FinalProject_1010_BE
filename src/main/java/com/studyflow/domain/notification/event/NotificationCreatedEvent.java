package com.studyflow.domain.notification.event;

import com.studyflow.domain.notification.enums.NotificationType;

// 알림 생성 이벤트 — 각 서비스가 트랜잭션 안에서 발행
// lazy 연관값(이름·수업명 등)은 발행 시점에 원시값으로 담아 리스너에서 추가 DB 접근 없이 저장
public record NotificationCreatedEvent(
        Long recipientId,
        NotificationType type,
        String title,
        String message,
        Long relatedId
) {
}
