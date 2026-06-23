package com.studyflow.domain.subscription.entity;

import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "user_subscriptions")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSubscription extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubscriptionType type;

    @Column(nullable = false)
    private long priceMileage;

    @Column(nullable = false)
    private LocalDateTime startsAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    public static UserSubscription create(User user, SubscriptionType type, LocalDateTime startsAt, LocalDateTime expiresAt) {
        UserSubscription subscription = new UserSubscription();
        subscription.user = user;
        subscription.type = type;
        subscription.priceMileage = type.getPriceMileage();
        subscription.startsAt = startsAt;
        subscription.expiresAt = expiresAt;
        return subscription;
    }

    public boolean isActive(LocalDateTime now) {
        return !expiresAt.isBefore(now);
    }
}
