package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.dto.response.ClassroomChatResponse;
import com.studyflow.domain.classroom.service.ClassroomChatService;
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

import java.util.List;

/**
 * 강의실 채팅 이력 조회 REST API (apidetail.md 23-1).
 *
 * <p>입장 시 과거 메시지를 한 번 불러오는 용도. 실시간 송수신은 WebSocket(23-2)이 담당한다.</p>
 */
@Tag(name = "강의실 채팅", description = "실시간 강의실 채팅 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomChatController {

    private final ClassroomChatService classroomChatService;

    @Operation(summary = "강의실 채팅 조회", description = "수업 멤버만. 세션의 채팅을 시간순으로 반환합니다.")
    @GetMapping("/classroom-sessions/{sessionId}/chats")
    public ResponseEntity<List<ClassroomChatResponse>> getChats(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(classroomChatService.getChats(sessionId, userId));
    }
}
