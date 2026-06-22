package com.studyflow.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 멀티 인스턴스에서 세션 단위 상태(화이트보드/오디오)의 read-modify-write를 직렬화하기 위한 가벼운 Redis 분산 락.
 *
 * <p>SET key token NX PX(만료) 로 획득, 토큰 일치 시에만 DEL 하는 Lua 스크립트로 해제(다른 소유자 락 오삭제 방지).
 * 만료(leaseMs)가 있어 보유자가 죽어도 자동 해제된다. 외부 라이브러리(Redisson) 없이 표준 패턴으로 구현.</p>
 */
@Component
@RequiredArgsConstructor
public class RedisStateLock {

    private final StringRedisTemplate redisTemplate;

    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private static final long LEASE_MS = 5000;   // 락 자동 만료(보유자 사망 대비)
    private static final long WAIT_MS = 3000;     // 최대 대기(초과 시 best-effort로 진행)
    private static final long SPIN_MS = 15;

    /** key 단위 락을 잡고 action 실행 후 해제. 반환값 전달. */
    public <T> T withLock(String key, Supplier<T> action) {
        String lockKey = "lock:" + key;
        String token = UUID.randomUUID().toString();
        boolean acquired = acquire(lockKey, token);
        try {
            return action.get();
        } finally {
            if (acquired) {
                try {
                    redisTemplate.execute(UNLOCK_SCRIPT, List.of(lockKey), token);
                } catch (Exception ignore) {
                    // 해제 실패해도 LEASE_MS 후 자동 만료되므로 무시
                }
            }
        }
    }

    private boolean acquire(String lockKey, String token) {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        while (true) {
            Boolean ok = redisTemplate.opsForValue().setIfAbsent(lockKey, token, Duration.ofMillis(LEASE_MS));
            if (Boolean.TRUE.equals(ok)) {
                return true;
            }
            if (System.currentTimeMillis() >= deadline) {
                return false; // 대기 초과 — best-effort로 진행(데드락 방지)
            }
            try {
                Thread.sleep(SPIN_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
    }
}
