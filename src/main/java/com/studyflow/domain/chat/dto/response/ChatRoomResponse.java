package com.studyflow.domain.chat.dto.response;

import com.studyflow.domain.chat.enums.ChatRoomType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 채팅방 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {

    private Long roomId;

    private ChatRoomType roomType;

    private Long courseId;

    private List<ChatParticipantResponse> participants;

    private ChatMessageResponse lastMessage;

    private LocalDateTime lastMessageAt;

    private long unreadCount;

    private LocalDateTime createdAt;
}