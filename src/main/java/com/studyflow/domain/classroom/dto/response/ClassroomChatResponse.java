package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.classroom.entity.ClassroomChat;

import java.time.LocalDateTime;

/**
 * 강의실 채팅 응답(23장) — 조회(REST)·실시간 수신(WebSocket) 공용.
 */
public record ClassroomChatResponse(
        Long chatId,
        Long sessionId,
        Long senderId,
        String senderName,
        String content,
        LocalDateTime createdAt
) {
    public static ClassroomChatResponse from(ClassroomChat c) {
        return new ClassroomChatResponse(
                c.getId(),
                c.getSession().getId(),
                c.getSender().getId(),
                c.getSender().getName(),
                c.getContent(),
                c.getCreatedAt()
        );
    }
}
