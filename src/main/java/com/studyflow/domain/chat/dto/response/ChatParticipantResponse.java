package com.studyflow.domain.chat.dto.response;

import com.studyflow.domain.chat.enums.ChatParticipantType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅방 참여자 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatParticipantResponse {

    private Long userId;

    private String name;

    private ChatParticipantType participantType;
}