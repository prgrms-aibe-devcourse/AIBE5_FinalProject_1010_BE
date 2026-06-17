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

    /**
     * 알림 1건을 저장하고, 트랜잭션 커밋 이후 수신자에게 실시간(STOMP)으로 push 한다.
     *
     * <p><b>왜 REQUIRES_NEW 인가</b><br>
     * 이 메서드는 {@code NotificationEventListener}가 {@code @TransactionalEventListener(AFTER_COMMIT)}
     * 에서 호출한다. AFTER_COMMIT 시점은 "원(原) 트랜잭션이 커밋됐지만 cleanupAfterCompletion() 직전"이라
     * TransactionSynchronizationManager에 이미 종료된 구 트랜잭션 컨텍스트가 남아 있다.
     * 이때 기본 전파(REQUIRED)로 save 하면 "기존(=종료된) 트랜잭션에 참여"로 처리돼 실제 INSERT가 일어나지 않는다.
     * 그래서 {@code REQUIRES_NEW}로 항상 새 트랜잭션을 열어 확실히 저장되게 한다.
     *
     * <p><b>왜 push를 afterCommit으로 미루는가 (코드리뷰 #155 반영)</b><br>
     * save 직후(=이 REQUIRES_NEW 트랜잭션이 아직 커밋되기 전)에 push 하면, push를 받은 클라이언트가
     * 곧바로 그 알림 ID로 {@code PATCH /notifications/{id}/read} 등을 호출할 때 DB에는 아직 커밋 전이라
     * 404가 날 수 있다(미세 경합). 그래서 push를 이 트랜잭션의 {@code afterCommit} 콜백으로 등록해
     * "DB 커밋 완료 → 그 다음 push" 순서를 보장한다.
     *
     * <p>전달할 {@link NotificationResponse}는 커밋 이후 콜백에서 엔티티 lazy 필드에 접근하지 않도록
     * save 직후 미리 DTO로 변환해 캡처한다.
     *
     * <p>TODO: 알림 누적 정리 정책 미구현 — 장기적으로 "N일 경과 또는 읽음 처리된 알림 삭제" 배치 필요.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(Long recipientId, NotificationType type, String title, String message, Long relatedId) {
        User recipient = userRepository.getReferenceById(recipientId);
        Notification saved = notificationRepository.save(Notification.create(recipient, type, title, message, relatedId));
        NotificationResponse payload = NotificationResponse.from(saved); // 커밋 후 lazy 접근 없도록 미리 DTO로 캡처

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 정상 경로: 이 REQUIRES_NEW 트랜잭션이 커밋된 직후에 push 가 실행된다.
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    push(recipientId, payload);
                }
            });
        } else {
            // 방어적 분기: 트랜잭션 동기화가 비활성인 예외 상황(예: 트랜잭션 밖 직접 호출)에서는 즉시 전송.
            push(recipientId, payload);
        }
    }

    /**
     * 실시간 push — 수신자 본인에게만 전송한다.
     * 서버는 {@code /user/{userId}/sub/notifications}로 보내고, 클라는 {@code /user/sub/notifications}를 구독한다
     * (Spring의 user-destination 변환. principal name = userId 는 CONNECT 시 인터셉터가 셋팅).
     *
     * <p>push 는 best-effort 다. 전송이 실패해도 알림은 이미 DB에 영속화돼 있어 REST 조회/폴링으로 복구되므로,
     * 예외를 흡수하고 경고 로그만 남겨 호출 흐름(강의실 열기·답변 채택 등)을 끊지 않는다.
     */
    private void push(Long recipientId, NotificationResponse payload) {
        try {
            messagingTemplate.convertAndSendToUser(String.valueOf(recipientId), "/sub/notifications", payload);
        } catch (Exception e) {
            log.warn("알림 실시간 push 실패 (recipientId={}): {}", recipientId, e.getMessage());
        }
    }
}
