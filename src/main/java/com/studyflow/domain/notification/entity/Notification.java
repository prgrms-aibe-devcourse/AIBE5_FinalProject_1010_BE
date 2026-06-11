package com.studyflow.domain.notification.entity;

import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 알림 엔티티 — 수강신청·QnA 답변 등 이벤트마다 수신자에게 1건 생성됨
@Entity
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_recipient", columnList = "recipient_id"),
                @Index(name = "idx_notification_recipient_read", columnList = "recipient_id, is_read")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 알림 수신자 (지연 로딩)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipient_id", nullable = false)
    private User recipient;

    // 소유권 검증 시 LAZY 로딩 없이 비교하기 위해 FK를 직접 노출 (읽기 전용)
    @Column(name = "recipient_id", insertable = false, updatable = false)
    private Long recipientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Getter(AccessLevel.NONE) // boolean isRead 에 @Getter 적용 시 isIsRead() 가 생성되는 문제 방지
    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    public boolean isRead() { return isRead; }

    // 클릭 시 이동 대상 ID (예: questionId). 없을 수 있음
    @Column(name = "related_id")
    private Long relatedId;

    // 알림 생성 — 수신자/타입/제목/메시지는 발행 시점에 원시값으로 해석해 전달받는다 (lazy 접근 없음)
    public static Notification create(User recipient, NotificationType type,
                                      String title, String message, Long relatedId) {
        Notification n = new Notification();
        n.recipient = recipient;
        n.type = type;
        n.title = title;
        n.message = message;
        n.relatedId = relatedId;
        return n;
    }

    public void markAsRead() {
        this.isRead = true;
    }
}
