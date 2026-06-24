package com.studyflow.domain.classroom.dto.response;

/**
 * 강의실 채팅 좋아요 변경 브로드캐스트 — 해당 메시지의 최신 좋아요 수.
 * 구독: /sub/classroom-sessions/{sessionId}/chat-likes
 */
public record ClassroomChatLikeResponse(
        Long chatId,
        long likeCount
) {
}
