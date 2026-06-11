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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // 내 알림 목록 조회 (최신순, 페이징)
    public Page<NotificationResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId, pageable)
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

        if (!notification.getRecipient().getId().equals(userId)) {
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

    // 알림 저장 — 커밋 이후 리스너가 별도 트랜잭션으로 호출 (REQUIRES_NEW)
    // 모든 값은 발행 시점에 원시값으로 담겨오므로 여기서는 수신자 프록시만 붙여 저장
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long recipientId, NotificationType type, String title, String message, Long relatedId) {
        User recipient = userRepository.getReferenceById(recipientId);
        notificationRepository.save(Notification.create(recipient, type, title, message, relatedId));
    }
}
