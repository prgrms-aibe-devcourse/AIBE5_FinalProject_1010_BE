package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.dto.request.LivekitTokenRequest;
import com.studyflow.domain.classroom.dto.response.ClassroomCloseResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomCurrentResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomParticipantResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomSessionResponse;
import com.studyflow.domain.classroom.dto.response.LivekitPreviewTokenResponse;
import com.studyflow.domain.classroom.dto.response.LivekitTokenResponse;
import com.studyflow.domain.classroom.service.ClassroomService;
import com.studyflow.domain.file.dto.response.FileUploadResponse;
import com.studyflow.domain.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final FileService fileService;

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
                .body(classroomService.joinSessionSafe(sessionId, userId));
    }

    // 세션 참가자 목록 — 수업 멤버. 선생님 판서 권한 토글 UI(roster)용 (이슈 #99/#162).
    @Operation(summary = "세션 참가자 목록", description = "수업 멤버만. 참가자별 participantId·userId·권한(canDraw 등)을 반환합니다.")
    @GetMapping("/classroom-sessions/{sessionId}/participants")
    public ResponseEntity<java.util.List<ClassroomParticipantResponse>> getParticipants(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(classroomService.getParticipants(sessionId, userId));
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

    // 미리보기 토큰 발급 — 비로그인 포함 누구나 진행 중인 강의실을 60초간 보기 전용으로 미리볼 수 있다.
    // 멤버십·로그인 검증 없이 발급하지만 canPublish=false·canPublishData=false·TTL 60초로 제한된다.
    @Operation(summary = "강의실 미리보기 토큰 발급",
            description = "비로그인 포함 누구나 진행 중인 강의실을 60초간 보기 전용으로 미리볼 수 있는 토큰을 발급합니다.")
    @PostMapping("/classroom-sessions/{sessionId}/livekit-preview-token")
    public ResponseEntity<LivekitPreviewTokenResponse> issuePreviewToken(@PathVariable Long sessionId) {
        return ResponseEntity.ok(classroomService.issuePreviewToken(sessionId));
    }

    // 미리보기용 화이트보드 스냅샷 — 비로그인 포함 공개. OPEN 세션의 현재 판서 상태를 1회 받아 동기화한다.
    @Operation(summary = "강의실 미리보기 화이트보드 스냅샷",
            description = "비로그인 포함 공개. 진행 중인 강의실의 현재 화이트보드 상태(pages+seq)를 반환합니다.")
    @GetMapping("/classroom-sessions/{sessionId}/whiteboard-preview")
    public ResponseEntity<java.util.Map<String, Object>> getWhiteboardPreview(@PathVariable Long sessionId) {
        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("board", classroomService.getWhiteboardPreviewSnapshot(sessionId));
        return ResponseEntity.ok(body);
    }

    @Operation(summary = "강의실 PDF 자료 업로드", description = "강의실을 연 담당 선생님만 PDF 자료를 업로드할 수 있습니다.")
    @PostMapping("/classroom-sessions/{sessionId}/documents")
    public ResponseEntity<FileUploadResponse> uploadClassroomDocument(
            @PathVariable Long sessionId,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        classroomService.verifyClassroomHost(sessionId, userId);
        return ResponseEntity.ok(fileService.uploadClassroomDocument(userId, file));
    }

    @Operation(summary = "강의실 듣기 자료(오디오) 업로드", description = "강의실을 연 담당 선생님만 오디오(mp3/wav 등) 자료를 업로드할 수 있습니다. (이슈 #182)")
    @PostMapping("/classroom-sessions/{sessionId}/audios")
    public ResponseEntity<FileUploadResponse> uploadClassroomAudio(
            @PathVariable Long sessionId,
            @RequestPart("file") MultipartFile file,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        classroomService.verifyClassroomHost(sessionId, userId);
        return ResponseEntity.ok(fileService.uploadClassroomAudio(userId, file));
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

    // 호스트 하트비트 — 선생님 FE가 강의실에 있는 동안 주기적으로 호출(부재 자동종료 타이머 리셋).
    // 응답 status가 CLOSED면 이미 종료된 것이므로 클라가 강의실에서 나간다.
    @Operation(summary = "강의실 하트비트", description = "호스트가 접속 중임을 알려 자동종료 타이머를 리셋. 응답 status=OPEN/CLOSED.")
    @PostMapping("/classroom-sessions/{sessionId}/heartbeat")
    public ResponseEntity<java.util.Map<String, String>> heartbeat(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(java.util.Map.of("status", classroomService.heartbeat(sessionId, userId)));
    }
}
