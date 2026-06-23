package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.ClassroomQuizService;
import com.studyflow.global.realtime.RealtimeBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * 강의실 실시간 문제풀이 WebSocket 컨트롤러.
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/quiz.
 * 전체 이벤트 구독: /sub/classroom-sessions/{sessionId}/quiz.
 * 개인 제출 결과 구독: /user/sub/classroom-sessions/{sessionId}/quiz-result.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomQuizWebSocketController {

    private final ClassroomQuizService quizService;
    private final RealtimeBroadcaster broadcaster;

    @MessageMapping("/classroom-sessions/{sessionId}/quiz")
    public void handle(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        if (principal == null) return;

        Long userId = Long.valueOf(principal.getName());
        String type = str(message.get("type"));
        try {
            switch (type) {
                case "start" -> start(sessionId, userId, message);
                case "submit" -> submit(sessionId, userId, message);
                case "end" -> end(sessionId, userId);
                default -> sendError(userId, sessionId, "알 수 없는 문제풀이 메시지입니다.");
            }
        } catch (Exception e) {
            log.debug("[classroom-quiz] 메시지 처리 실패(sessionId={}, userId={}, type={})", sessionId, userId, type, e);
            sendError(userId, sessionId, e.getMessage() != null ? e.getMessage() : "문제풀이 요청 처리에 실패했습니다.");
        }
    }

    private void start(Long sessionId, Long userId, Map<String, Object> message) {
        Map<String, Object> payload = quizService.start(
                sessionId,
                userId,
                str(message.get("question")),
                str(message.get("answer")),
                intVal(message.get("durationSec"))
        );
        broadcaster.send(topic(sessionId), payload);
        scheduleEnd(sessionId, str(payload.get("quizId")), longVal(payload.get("endsAtMs")));
    }

    private void submit(Long sessionId, Long userId, Map<String, Object> message) {
        Map<String, Object> result = quizService.submit(
                sessionId,
                userId,
                str(message.get("quizId")),
                str(message.get("answer"))
        );
        result.put("type", "submitted");
        broadcaster.sendToUser(String.valueOf(userId), userDestination(sessionId), result);
        broadcaster.send(topic(sessionId), Map.of(
                "type", "submissionUpdate",
                "quizId", str(message.get("quizId")),
                "serverNowMs", System.currentTimeMillis()
        ));
    }

    private void end(Long sessionId, Long userId) {
        broadcaster.send(topic(sessionId), quizService.end(sessionId, userId));
    }

    private void scheduleEnd(Long sessionId, String quizId, Long endsAtMs) {
        if (quizId == null || endsAtMs == null) return;
        long delayMs = Math.max(0L, endsAtMs - System.currentTimeMillis());
        CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS).execute(() -> {
            Map<String, Object> ended = quizService.endIfSame(sessionId, quizId);
            if (ended != null) {
                broadcaster.send(topic(sessionId), ended);
            }
        });
    }

    private void sendError(Long userId, Long sessionId, String message) {
        broadcaster.sendToUser(
                String.valueOf(userId),
                userDestination(sessionId),
                Map.of("type", "error", "message", message)
        );
    }

    private static String topic(Long sessionId) {
        return "/sub/classroom-sessions/" + sessionId + "/quiz";
    }

    private static String userDestination(Long sessionId) {
        return "/sub/classroom-sessions/" + sessionId + "/quiz-result";
    }

    private static String str(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static Integer intVal(Object value) {
        if (value instanceof Number n) return n.intValue();
        if (value == null) return null;
        try { return Integer.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }

    private static Long longVal(Object value) {
        if (value instanceof Number n) return n.longValue();
        if (value == null) return null;
        try { return Long.valueOf(String.valueOf(value)); } catch (NumberFormatException e) { return null; }
    }
}
