package com.studyflow.domain.notification.service;

import com.studyflow.domain.notification.dto.response.NotificationResponse;
import com.studyflow.domain.notification.entity.Notification;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.exception.NotificationException;
import com.studyflow.domain.notification.repository.NotificationRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 내 알림 목록 조회 (최신순, 페이징)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientId(userId, pageable)
                .map(NotificationResponse::from);
    }

    // 안 읽은 알림 수 (벨 뱃지 표시용)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(userId);
    }

    // 단건 읽음 처리 — 본인 알림인지 확인 후 처리
    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_NOT_FOUND, ErrorCode.NOTIFICATION_NOT_FOUND.getMessage()));

        if (!notification.getRecipientId().equals(userId)) {
            throw new NotificationException(
                    ErrorCode.NOTIFICATION_ACCESS_FORBIDDEN, ErrorCode.NOTIFICATION_ACCESS_FORBIDDEN.getMessage());
        }

        notification.markAsRead();
    }

    // 전체 읽음 처리
    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllReadByRecipientId(userId);
    }

    // 알림 저장 — 커밋 이후 AFTER_COMMIT 단계에서 리스너가 호출할 때는 이미 원 트랜잭션이 없으므로
    // REQUIRES_NEW 없이 @Transactional 만으로 동일하게 새 트랜잭션에서 저장됨
    // TODO: 알림 누적 정리 정책 미구현 — 장기적으로 "N일 경과 또는 읽음 처리된 알림 삭제" 배치 필요
    @Transactional
    public void create(Long recipientId, NotificationType type, String title, String message, Long relatedId) {
        User recipient = userRepository.getReferenceById(recipientId);
        notificationRepository.save(Notification.create(recipient, type, title, message, relatedId));
    }
}
