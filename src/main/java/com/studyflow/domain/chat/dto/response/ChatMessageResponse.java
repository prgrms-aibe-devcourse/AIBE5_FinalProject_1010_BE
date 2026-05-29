package com.studyflow.domain.chat.dto.response;


import com.studyflow.domain.chat.enums.ChatMessageType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅 메시지 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    private Long messageId;

    private Long roomId;

    private Long senderId;

    private String senderName;

    private ChatMessageType messageType;

    private String content;

    private List<ChatAttachmentResponse> attachments;

    private boolean deleted;

    private LocalDateTime sentAt;

    private LocalDateTime createdAt;
}