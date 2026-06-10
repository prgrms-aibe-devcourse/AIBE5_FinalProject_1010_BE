package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.dto.request.LivekitTokenRequest;
import com.studyflow.domain.classroom.dto.response.ClassroomCloseResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomCurrentResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomParticipantResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomSessionResponse;
import com.studyflow.domain.classroom.dto.response.LivekitTokenResponse;
import com.studyflow.domain.classroom.service.ClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 강의실(실시간 화상수업) 세션 API — apidetail.md 22장.
 *
 * <p>열기/종료는 담당 선생님, 조회/참가는 수업 멤버(담당교사·ACTIVE 수강생)만 가능하며
 * 멤버십·소유권 검증은 서비스에서 처리한다.</p>
 */
@Tag(name = "강의실", description = "실시간 화상수업 세션 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomSessionController {

    private final ClassroomService classroomService;

    // 22-1 강의실 열기 — 담당 선생님 전용
    @Operation(summary = "강의실 열기", description = "담당 선생님 전용. 이미 열린 강의실이 있으면 그 세션을 반환합니다.")
    @PostMapping("/courses/{courseId}/classroom-sessions")
    public ResponseEntity<ClassroomSessionResponse> openSession(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classroomService.openSession(courseId, userId));
    }

    // 22-2 현재 강의실 조회 — 수업 멤버
    @Operation(summary = "현재 강의실 조회", description = "수업 멤버(담당교사·수강생)만 조회 가능. 열린 강의실이 없으면 404.")
    @GetMapping("/courses/{courseId}/classroom-sessions/current")
    public ResponseEntity<ClassroomCurrentResponse> getCurrentSession(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(classroomService.getCurrentSession(courseId, userId));
    }

    // 22-3 강의실 참가 — 수업 멤버
    @Operation(summary = "강의실 참가", description = "수업 멤버만 참가 가능. 재입장 시 기존 참가 정보를 반환합니다.")
    @PostMapping("/classroom-sessions/{sessionId}/participants")
    public ResponseEntity<ClassroomParticipantResponse> joinSession(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(classroomService.joinSession(sessionId, userId));
    }

    // 22-4 LiveKit 토큰 발급 — 수업 멤버. 입장 시점마다 즉석 발급(저장 안 함).
    @Operation(summary = "LiveKit 토큰 발급", description = "수업 멤버만. 송출 권한(canPublish)이 토큰에 반영됩니다.")
    @PostMapping("/classroom-sessions/{sessionId}/livekit-token")
    public ResponseEntity<LivekitTokenResponse> issueLivekitToken(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestBody(required = false) LivekitTokenRequest request
    ) {
        return ResponseEntity.ok(classroomService.issueLivekitToken(sessionId, userId, request));
    }

    // 22-6 강의실 종료 — 담당 선생님 전용
    @Operation(summary = "강의실 종료", description = "담당 선생님 전용. 진행 시간(durationSeconds)이 계산됩니다.")
    @PatchMapping("/classroom-sessions/{sessionId}/close")
    public ResponseEntity<ClassroomCloseResponse> closeSession(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(classroomService.closeSession(sessionId, userId));
    }
}
