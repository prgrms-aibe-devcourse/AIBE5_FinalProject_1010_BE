package com.studyflow.domain.classroom.dto.response;

import com.studyflow.domain.chat.dto.response.ChatAttachmentResponse;
import com.studyflow.domain.chat.enums.ChatMessageType;
import com.studyflow.domain.classroom.entity.ClassroomChat;
import com.studyflow.domain.file.entity.FileAsset;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * 강의실 채팅 응답(23장) — 조회(REST)·실시간 수신(WebSocket) 공용.
 * 1:1 채팅과 동일하게 messageType + 이미지 첨부(attachments)를 포함한다.
 */
public record ClassroomChatResponse(
        Long chatId,
        Long sessionId,
        Long senderId,
        String senderName,
        ChatMessageType messageType,
        String content,
        List<ChatAttachmentResponse> attachments,
        LocalDateTime createdAt
) {
    public static ClassroomChatResponse from(ClassroomChat c) {
        List<ChatAttachmentResponse> attachments = c.getAttachments().stream()
                .sorted(Comparator.comparingInt(a -> a.getSortOrder() == null ? 0 : a.getSortOrder()))
                .map(a -> {
                    FileAsset f = a.getFileAsset();
                    return new ChatAttachmentResponse(
                            f.getId(), f.getFileUrl(), f.getThumbnailUrl(), f.getOriginalFileName(),
                            f.getContentType(), f.getFileSize(), f.getWidth(), f.getHeight(), a.getSortOrder());
                })
                .toList();
        return new ClassroomChatResponse(
                c.getId(),
                c.getSession().getId(),
                c.getSender().getId(),
                c.getSender().getName(),
                c.getMessageType() == null ? ChatMessageType.TEXT : c.getMessageType(),
                c.getContent(),
                attachments,
                c.getCreatedAt()
        );
    }
}
