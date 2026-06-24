package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.repository.ClassroomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * 세션별 "화이트보드 판서 권한자(userId 집합)"를 Redis에 보관한다 (이슈 #162, 멀티 인스턴스 대응).
 *
 * <p>화이트보드 op/live 메시지는 고빈도라 메시지마다 DB 조회는 부담이 크다. 그래서 권한자 집합을
 * Redis Set으로 캐시하고 WS relay는 이 캐시로 권한을 검사한다. 멀티 인스턴스에서 모든 인스턴스가
 * 같은 Redis Set을 보므로, 한 인스턴스에서 grant/revoke 하면 전 인스턴스에 즉시 반영된다.</p>
 *
 * <p>캐시 미스(키 없음, 예: 첫 접근/만료) 시 DB의 현재 권한자 목록으로 1회 적재한다(loaded 플래그로 구분).</p>
 */
@Component
@RequiredArgsConstructor
public class WhiteboardDrawPermissionStore {

    private final ClassroomParticipantRepository participantRepository;
    private final StringRedisTemplate redisTemplate;

    private static final Duration LOADED_TTL = Duration.ofHours(12);

    private String key(Long sessionId)       { return "wb:drawers:" + sessionId; }
    private String loadedKey(Long sessionId)  { return "wb:drawers:loaded:" + sessionId; }

    /** 해당 사용자가 이 세션에서 판서할 수 있는지. */
    public boolean canDraw(Long sessionId, Long userId) {
        ensureLoaded(sessionId);
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key(sessionId), String.valueOf(userId)));
    }

    /** 판서 권한 부여 — 입장(canDraw=true) / 선생님의 권한 부여 시. */
    public void grant(Long sessionId, Long userId) {
        ensureLoaded(sessionId);
        redisTemplate.opsForSet().add(key(sessionId), String.valueOf(userId));
    }

    /** 판서 권한 회수 — 선생님의 권한 회수 시. */
    public void revoke(Long sessionId, Long userId) {
        ensureLoaded(sessionId);
        redisTemplate.opsForSet().remove(key(sessionId), String.valueOf(userId));
    }

    /** 세션 종료 시 캐시 제거. */
    public void clear(Long sessionId) {
        redisTemplate.delete(key(sessionId));
        redisTemplate.delete(loadedKey(sessionId));
    }

    /** 캐시 미스 시 DB의 현재 권한자 목록으로 1회 적재(loaded 플래그로 중복/공백셋 구분). */
    private void ensureLoaded(Long sessionId) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(loadedKey(sessionId)))) {
            return;
        }
        List<Long> ids = participantRepository.findDrawerUserIdsBySessionId(sessionId);
        if (!ids.isEmpty()) {
            String[] members = ids.stream().map(String::valueOf).toArray(String[]::new);
            redisTemplate.opsForSet().add(key(sessionId), members);
        }
        redisTemplate.opsForValue().set(loadedKey(sessionId), "1", LOADED_TTL);
    }
}
