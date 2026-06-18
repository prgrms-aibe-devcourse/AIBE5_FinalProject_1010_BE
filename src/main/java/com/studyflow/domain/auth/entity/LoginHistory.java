package com.studyflow.domain.auth.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "login_history",
        indexes = {
                @Index(name = "idx_login_history_user_id", columnList = "user_id"),
                @Index(name = "idx_login_history_login_at", columnList = "login_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "login_at", nullable = false, updatable = false)
    private LocalDateTime loginAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "browser", length = 100)
    private String browser;

    public static LoginHistory of(Long userId, String ipAddress, String deviceInfo, String browser) {
        LoginHistory history = new LoginHistory();
        history.userId = userId;
        history.loginAt = LocalDateTime.now();
        history.ipAddress = ipAddress;
        history.deviceInfo = deviceInfo;
        history.browser = browser;
        return history;
    }
}
