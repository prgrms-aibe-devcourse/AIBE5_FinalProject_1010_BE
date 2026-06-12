package com.studyflow.domain.notification.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.notification.entity.Notification;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationEventListener;
import com.studyflow.domain.notification.exception.NotificationException;
import com.studyflow.domain.notification.repository.NotificationRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.config.QuerydslConfig;
import com.studyflow.global.exception.ErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * NotificationService 삭제 인가 검증 — 404(미존재) / 403(타인 알림) 시나리오.
 * @Transactional(NOT_SUPPORTED) 로 DataJpaTest 의 자동 롤백을 비활성화해
 * 서비스 메서드가 독립 트랜잭션에서 실행되도록 한다.
 */
@DataJpaTest
@Import({QuerydslConfig.class, NotificationService.class, NotificationEventListener.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class NotificationServiceTest {

    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() { return new ObjectMapper(); }
    }

    @Autowired NotificationService notificationService;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("존재하지 않는 알림 삭제 시 404 예외")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void delete_notFound() {
        assertThatThrownBy(() -> notificationService.delete(1L, 999L))
                .isInstanceOf(NotificationException.class)
                .extracting(e -> ((NotificationException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @Test
    @DisplayName("타인 알림 삭제 시도 시 403 예외")
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void delete_forbidden() {
        // given
        User owner    = userRepository.save(User.createForTest("owner@test.com",    "소유자", UserRole.STUDENT));
        User attacker = userRepository.save(User.createForTest("attacker@test.com", "공격자", UserRole.STUDENT));
        Notification notification = notificationRepository.save(
                Notification.create(owner, NotificationType.ENROLLMENT_REQUESTED, "제목", "내용", null));

        // when / then
        assertThatThrownBy(() -> notificationService.delete(attacker.getId(), notification.getId()))
                .isInstanceOf(NotificationException.class)
                .extracting(e -> ((NotificationException) e).getErrorCode())
                .isEqualTo(ErrorCode.NOTIFICATION_ACCESS_FORBIDDEN);
    }
}
