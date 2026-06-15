package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.WhiteboardSnapshotStore;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 강의실 화이트보드 현재 스냅샷 조회 REST API (이슈 #131).
 *
 * <p>입장 시 현재까지 그려진 보드를 한 번 받아 동기화하는 용도. 실시간 변경은 WebSocket이 담당한다.
 * 보관된 스냅샷이 없으면 board=null(빈 보드로 시작).</p>
 */
@Tag(name = "강의실 화이트보드", description = "실시간 화이트보드 동기화 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomWhiteboardController {

    private final WhiteboardSnapshotStore snapshotStore;

    @Operation(summary = "화이트보드 현재 스냅샷 조회", description = "입장 시 현재 보드를 받아 동기화. 없으면 board=null.")
    @GetMapping("/classroom-sessions/{sessionId}/whiteboard")
    public ResponseEntity<Map<String, Object>> getSnapshot(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("board", snapshotStore.get(sessionId));
        return ResponseEntity.ok(body);
    }
}
