package com.studyflow.domain.subscription.entity;

import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 사용자별 구독(이용권). (userId, type) 당 1행. 결제 시 만료일을 연장한다.
 *
 * <p>활성 여부 = expiresAt가 현재보다 미래. 활성 중에 또 결제하면 현재 만료일에서 이어서 연장(스택),
 * 만료됐으면 지금부터 30일.</p>
 */
@Entity
@Table(name = "subscription", uniqueConstraints = {
        @UniqueConstraint(name = "uk_subscription_user_type", columnNames = {"user_id", "type"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionType type;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    private Subscription(Long userId, SubscriptionType type, LocalDateTime expiresAt) {
        this.userId = userId;
        this.type = type;
        this.expiresAt = expiresAt;
    }

    public static Subscription createFor(Long userId, SubscriptionType type, LocalDateTime expiresAt) {
        return new Subscription(userId, type, expiresAt);
    }

    public boolean isActive(LocalDateTime now) {
        return expiresAt != null && expiresAt.isAfter(now);
    }

    /** days만큼 연장. 활성 중이면 현재 만료일에서 이어서, 아니면 now 기준으로. */
    public void extend(LocalDateTime now, int days) {
        LocalDateTime base = isActive(now) ? expiresAt : now;
        this.expiresAt = base.plusDays(days);
    }
}
