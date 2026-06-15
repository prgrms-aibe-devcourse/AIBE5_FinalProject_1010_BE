package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.WhiteboardStateStore;
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
 * <p>입장 시(그리고 seq 구멍/재연결로 자가 치유가 필요할 때) 서버의 권위 상태를 한 번에 받아 동기화한다.
 * 실시간 변경은 WebSocket이 담당한다. 응답 board는 {pages:[{id,shapes}], seq} 형태이며 항상 존재한다(빈 보드는 page "p1" 1장).</p>
 */
@Tag(name = "강의실 화이트보드", description = "실시간 화이트보드 동기화 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomWhiteboardController {

    private final WhiteboardStateStore stateStore;

    @Operation(summary = "화이트보드 현재 상태 조회", description = "서버 권위 상태(pages+seq)를 받아 동기화. 입장/재동기화용.")
    @GetMapping("/classroom-sessions/{sessionId}/whiteboard")
    public ResponseEntity<Map<String, Object>> getSnapshot(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("board", stateStore.snapshot(sessionId));
        return ResponseEntity.ok(body);
    }
}
