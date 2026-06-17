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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

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

    // 단건 삭제 — 본인 알림인지 확인 후 삭제
    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotificationException(
                        ErrorCode.NOTIFICATION_NOT_FOUND, ErrorCode.NOTIFICATION_NOT_FOUND.getMessage()));

        if (!notification.getRecipientId().equals(userId)) {
            throw new NotificationException(
                    ErrorCode.NOTIFICATION_ACCESS_FORBIDDEN, ErrorCode.NOTIFICATION_ACCESS_FORBIDDEN.getMessage());
        }

        notificationRepository.delete(notification);
    }

    // 전체 삭제
    @Transactional
    public void deleteAll(Long userId) {
        notificationRepository.deleteAllByRecipientId(userId);
    }

    // AFTER_COMMIT 리스너가 호출하는 시점에는 원 트랜잭션이 커밋됐지만 cleanupAfterCompletion() 이전이라
    // TransactionSynchronizationManager에 구 트랜잭션이 아직 남아 있다.
    // REQUIRED 전파 시 "기존 트랜잭션 참여"로 처리돼 실제 저장이 안 되므로 REQUIRES_NEW 로 강제 신규 트랜잭션 개설.
    // TODO: 알림 누적 정리 정책 미구현 — 장기적으로 "N일 경과 또는 읽음 처리된 알림 삭제" 배치 필요
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long recipientId, NotificationType type, String title, String message, Long relatedId) {
        User recipient = userRepository.getReferenceById(recipientId);
        Notification saved = notificationRepository.save(Notification.create(recipient, type, title, message, relatedId));
        NotificationResponse payload = NotificationResponse.from(saved); // 커밋 후 lazy 접근 없도록 미리 DTO 변환

        // 실시간 push는 "이 트랜잭션이 커밋된 뒤"에 보낸다.
        // save 직후(커밋 전) 보내면, push를 받은 클라가 곧바로 알림 ID로 read/delete를 호출할 때
        // DB에 아직 커밋 전이라 404가 날 수 있다. afterCommit 콜백으로 순서를 보장한다.
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push(recipientId, payload);
                }
            });
        } else {
            push(recipientId, payload); // 트랜잭션 동기화가 없는 경우(예외적) 즉시 전송
        }
    }

    // 실시간 push — 수신자에게만(STOMP /user/{userId}/sub/notifications). 클라는 /user/sub/notifications 구독.
    // 실패해도 영속 알림은 남으므로(REST로 받음) 흐름 끊지 않게 예외를 삼킨다.
    private void push(Long recipientId, NotificationResponse payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(recipientId), "/sub/notifications", payload);
        } catch (Exception e) {
            log.warn("알림 실시간 push 실패 (recipientId={}): {}", recipientId, e.getMessage());
        }
    }
}
