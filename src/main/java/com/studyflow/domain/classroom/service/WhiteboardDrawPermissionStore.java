package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.repository.ClassroomParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 세션별 "화이트보드 판서 권한자(userId 집합)"를 메모리에 보관한다 (이슈 #162).
 *
 * <p>화이트보드 op/live 메시지는 고빈도(스트로크·포인터 이동마다)라 메시지마다 DB를 조회하면 부담이 크다.
 * 그래서 판서 권한자 집합을 메모리에 캐시하고, WS relay는 이 캐시로만 권한을 검사한다(메시지당 DB 미조회).</p>
 *
 * <p>캐시는 다음 시점에 갱신된다:
 * <ul>
 *   <li><b>입장</b> 시 canDraw=true면 grant — 선생님(호스트)은 입장 시 기본 canDraw=true, 학생은 false</li>
 *   <li>선생님이 <b>권한 변경</b> 시 grant/revoke</li>
 *   <li>세션 <b>종료</b> 시 clear</li>
 * </ul>
 * 캐시에 없는 세션(서버 재시작 등으로 메모리가 비었을 때)은 최초 접근 시 DB에서 권한자 목록을 lazy 로딩한다.</p>
 *
 * <p>단일 인스턴스 in-memory — 다중 인스턴스 확장 시 Redis 등으로 대체(WhiteboardStateStore와 동일 정책).</p>
 */
@Component
@RequiredArgsConstructor
public class WhiteboardDrawPermissionStore {

    private final ClassroomParticipantRepository participantRepository;

    /** sessionId -> 판서 가능한 userId 집합. 키가 없으면 "아직 미로딩"(최초 접근 시 DB lazy 로딩). */
    private final Map<Long, Set<Long>> drawersBySession = new ConcurrentHashMap<>();

    /** 해당 사용자가 이 세션에서 판서(화이트보드 그리기)할 수 있는지. */
    public boolean canDraw(Long sessionId, Long userId) {
        return drawers(sessionId).contains(userId);
    }

    /** 판서 권한 부여 — 입장(canDraw=true) / 선생님의 권한 부여 시. */
    public void grant(Long sessionId, Long userId) {
        drawers(sessionId).add(userId);
    }

    /** 판서 권한 회수 — 선생님의 권한 회수 시. */
    public void revoke(Long sessionId, Long userId) {
        drawers(sessionId).remove(userId);
    }

    /** 세션 종료 시 캐시 제거(메모리 누적 방지). */
    public void clear(Long sessionId) {
        drawersBySession.remove(sessionId);
    }

    private Set<Long> drawers(Long sessionId) {
        return drawersBySession.computeIfAbsent(sessionId, this::loadFromDb);
    }

    /** 캐시 미스(서버 재시작 등) 시 DB의 현재 권한자 목록으로 초기화한다. */
    private Set<Long> loadFromDb(Long sessionId) {
        Set<Long> set = ConcurrentHashMap.newKeySet();
        set.addAll(participantRepository.findDrawerUserIdsBySessionId(sessionId));
        return set;
    }
}
