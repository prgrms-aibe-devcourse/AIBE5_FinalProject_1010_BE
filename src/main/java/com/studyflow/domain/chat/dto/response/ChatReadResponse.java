package com.studyflow.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 읽음 처리 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadResponse {

    private Long roomId;

    private Long userId;

    private Long lastReadMessageId;

    private LocalDateTime lastReadAt;
}