package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.ClassroomService;
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
 * 강의실 듣기 자료(오디오) 현재 재생 상태 스냅샷 조회 REST API (이슈 #182).
 *
 * <p>입장/재연결 시 현재 트랙·재생여부·위치를 한 번에 받아 동기화한다. 실시간 변경은 WebSocket이 담당한다.
 * 응답 audio는 {url, fileName, fileId, playing, positionSec, updatedAtMs, serverNowMs} 형태(트랙 없으면 url=null).
 * 수업 멤버(담당 선생님 또는 ACTIVE 수강생)만 조회 가능 — 멤버십 검증은 서비스에서 수행한다(위반 시 403).</p>
 */
@Tag(name = "강의실 오디오", description = "듣기 자료 재생 동기화 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomAudioController {

    private final ClassroomService classroomService;

    @Operation(summary = "오디오 현재 상태 조회", description = "현재 트랙/재생여부/위치를 받아 동기화. 입장/재연결용. 수업 멤버만.")
    @GetMapping("/classroom-sessions/{sessionId}/audio")
    public ResponseEntity<Map<String, Object>> getSnapshot(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        Map<String, Object> body = new HashMap<>();
        body.put("audio", classroomService.getAudioSnapshot(sessionId, userId));
        return ResponseEntity.ok(body);
    }
}
