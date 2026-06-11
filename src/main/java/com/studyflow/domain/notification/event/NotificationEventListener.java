package com.studyflow.domain.notification.event;

import com.studyflow.domain.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

// 알림 이벤트 리스너 — 커밋 이후(AFTER_COMMIT) 실행되어 핵심 트랜잭션 롤백에 영향 없음
// 알림 저장 실패 시 예외를 삼켜 사용자 흐름이 끊기지 않게 함
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final NotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(NotificationCreatedEvent event) {
        try {
            notificationService.create(
                    event.recipientId(), event.type(),
                    event.title(), event.message(), event.relatedId());
        } catch (Exception e) {
            log.warn("알림 저장 실패 (recipientId={}, type={}): {}",
                    event.recipientId(), event.type(), e.getMessage(), e);
        }
    }
}
