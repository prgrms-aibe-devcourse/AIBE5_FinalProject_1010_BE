package com.studyflow.global.config;

import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.redis.spring.RedisLockProvider;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * ShedLock 설정 — 멀티 인스턴스에서 {@code @Scheduled} 작업의 중복 실행을 막는다 (이슈 #135 후속).
 *
 * <p>EC2 2대가 동시에 같은 스케줄러를 돌리면 같은 세션을 중복 종료(종료 이벤트 중복 송신)할 수 있다.
 * 모든 인스턴스가 공유하는 Redis에 락을 잡아, 한 번에 한 인스턴스만 실행하게 한다.
 * {@code @SchedulerLock}이 붙은 메서드에만 적용된다.</p>
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT5M")
public class SchedulerLockConfig {

    @Bean
    public LockProvider lockProvider(RedisConnectionFactory connectionFactory) {
        return new RedisLockProvider(connectionFactory);
    }
}
