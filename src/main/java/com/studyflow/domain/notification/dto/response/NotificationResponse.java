package com.studyflow.domain.notification.dto.response;

import com.studyflow.domain.notification.entity.Notification;
import com.studyflow.domain.notification.enums.NotificationType;

import java.time.LocalDateTime;

// 알림 목록 조회 응답 DTO
public record NotificationResponse(
        Long id,
        NotificationType type,
        String title,
        String message,
        boolean isRead,
        Long relatedId,
        LocalDateTime createdAt
) {
    // 엔티티 → DTO 변환
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.isRead(),
                n.getRelatedId(),
                n.getCreatedAt()
        );
    }
}
