package com.studyflow.domain.classroom.dto.request;

/**
 * 강의실 채팅 좋아요 토글 요청 — WebSocket payload.
 * 발행: /pub/classroom-sessions/{sessionId}/chat-likes
 */
public record ClassroomChatLikeRequest(
        Long chatId
) {
}
