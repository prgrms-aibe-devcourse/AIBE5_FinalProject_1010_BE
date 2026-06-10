package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.dto.request.ParticipantPermissionUpdateRequest;
import com.studyflow.domain.classroom.dto.response.ParticipantPermissionResponse;
import com.studyflow.domain.classroom.service.ClassroomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 강의실 참가자 권한 API — apidetail.md 22-5.
 *
 * <p>담당 선생님이 학생의 판서/화면공유/채팅 권한을 변경한다.</p>
 */
@Tag(name = "강의실 참가자", description = "강의실 참가자 권한 관리 API")
@RestController
@RequestMapping("/api/v1/classroom-participants")
@RequiredArgsConstructor
public class ClassroomParticipantController {

    private final ClassroomService classroomService;

    // 22-5 참가자 권한 변경 — 담당 선생님 전용
    @Operation(summary = "참가자 권한 변경", description = "담당 선생님 전용. 참가자의 판서/화면공유/채팅 권한을 변경합니다.")
    @PatchMapping("/{participantId}/permissions")
    public ResponseEntity<ParticipantPermissionResponse> updatePermissions(
            @PathVariable Long participantId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody ParticipantPermissionUpdateRequest request
    ) {
        return ResponseEntity.ok(classroomService.updatePermissions(participantId, userId, request));
    }
}
