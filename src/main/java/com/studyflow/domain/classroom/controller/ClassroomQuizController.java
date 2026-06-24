package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.ClassroomQuizService;
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

import java.util.Map;

@Tag(name = "강의실 문제풀이", description = "실시간 문제풀이 현재 상태 조회 API")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ClassroomQuizController {

    private final ClassroomQuizService quizService;

    @Operation(summary = "문제풀이 현재 상태 조회", description = "입장/재연결/타이머 종료 시 현재 문제, 제출 여부, 종료 결과를 조회합니다.")
    @GetMapping("/classroom-sessions/{sessionId}/quiz")
    public ResponseEntity<Map<String, Object>> getSnapshot(
            @PathVariable Long sessionId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(Map.of("quiz", quizService.snapshot(sessionId, userId)));
    }
}
