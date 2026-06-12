package com.studyflow.domain.notification.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.repository.NotificationRepository;
import com.studyflow.domain.notification.service.NotificationService;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.config.QuerydslConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 회귀 방지 통합 테스트 — "트랜잭션 커밋 → AFTER_COMMIT 리스너 → Notification row 저장" 전 과정을 검증한다.
 *
 * 과거 NotificationService.create()의 전파 속성을 REQUIRES_NEW → REQUIRED 로 바꾸자
 * AFTER_COMMIT 시점에 알림이 통째로 저장되지 않던 버그가 있었다. 단위 테스트로는 전파 동작을 잡을 수 없어,
 * 실제 트랜잭션 매니저로 커밋을 일으켜 리스너가 새 트랜잭션에서 row를 저장하는지 확인한다.
 * REQUIRED 로 되돌리면 이 테스트의 count 단언이 깨진다.
 */
@DataJpaTest
@Import({QuerydslConfig.class, NotificationService.class, NotificationEventListener.class})
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class NotificationEventListenerIntegrationTest {

    @Autowired ApplicationEventPublisher eventPublisher;
    @Autowired NotificationRepository notificationRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlatformTransactionManager transactionManager;

    // course 도메인의 JPA @Converter(NoticeAttachmentListConverter)가 ObjectMapper를 주입받으므로
    // @DataJpaTest 슬라이스에 ObjectMapper 빈을 보충해 컨텍스트 로딩을 가능하게 한다.
    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    @AfterEach
    void tearDown() {
        notificationRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("트랜잭션 커밋 후 AFTER_COMMIT 리스너가 알림 row를 저장한다 (REQUIRES_NEW 전파 회귀 방지)")
    @Transactional(propagation = Propagation.NOT_SUPPORTED) // @DataJpaTest 기본 롤백 트랜잭션 비활성화 → 실제 커밋 발생
    void afterCommitListenerPersistsNotification() {
        // given: 알림 수신자 유저 (recipient_id 는 NOT NULL FK 이므로 먼저 영속화)
        User recipient = persistUser();
        long before = notificationRepository.count();

        // when: 트랜잭션 안에서 이벤트를 발행하고 커밋 (커밋 시 AFTER_COMMIT 리스너 실행)
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.executeWithoutResult(status ->
                eventPublisher.publishEvent(new NotificationCreatedEvent(
                        recipient.getId(), NotificationType.ENROLLMENT_REQUESTED,
                        "새 수강 신청", "테스트 메시지", 1L)));

        // then: 리스너가 새 트랜잭션(REQUIRES_NEW)에서 알림을 저장
        assertThat(notificationRepository.count()).isEqualTo(before + 1);
    }

    private User persistUser() {
        return userRepository.save(User.createForTest("recipient@test.com", "수신자", UserRole.STUDENT));
    }
}
