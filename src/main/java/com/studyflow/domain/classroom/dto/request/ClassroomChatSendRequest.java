package com.studyflow.domain.classroom.dto.request;

/**
 * 강의실 채팅 전송(23-2) 요청 — WebSocket payload.
 *
 * <p>발행 경로: /pub/classroom-sessions/{sessionId}/chats</p>
 */
public record ClassroomChatSendRequest(
        String content
) {
}
