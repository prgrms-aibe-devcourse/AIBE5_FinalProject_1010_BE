package com.studyflow.domain.classroom.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 강의실 화이트보드의 "현재 보드 스냅샷"을 세션별로 메모리에 보관한다.
 *
 * <p>늦게 입장한 참가자가 현재까지 그려진 보드를 한 번에 받아 동기화할 수 있도록,
 * 클라이언트가 주기적으로 보내는 전체 보드(JSON)를 세션별 최신본으로 저장한다.
 * 실시간 op는 별도로 브로드캐스트되며 이 스토어엔 저장하지 않는다(스냅샷만 보관).
 * 단일 인스턴스 기준 in-memory — 다중 인스턴스 확장 시 Redis 등으로 대체.</p>
 */
@Component
public class WhiteboardSnapshotStore {

    private final Map<Long, Object> snapshots = new ConcurrentHashMap<>();

    public void put(Long sessionId, Object board) {
        if (board != null) snapshots.put(sessionId, board);
    }

    public Object get(Long sessionId) {
        return snapshots.get(sessionId);
    }

    public void clear(Long sessionId) {
        snapshots.remove(sessionId);
    }
}
