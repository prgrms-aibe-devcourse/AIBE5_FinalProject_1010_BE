package com.studyflow.domain.chat.dto.request;

import com.studyflow.domain.chat.enums.ChatMessageType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * WebSocket 채팅 메시지 전송 요청.
 *
 * TEXT:
 * - content 필수
 * - fileIds 비어 있음
 *
 * IMAGE:
 * - content는 캡션 또는 null
 * - fileIds 필수
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageSendRequest {

    @NotNull(message = "메시지 타입은 필수입니다.")
    private ChatMessageType messageType;

    private String content;

    private List<Long> fileIds;
}