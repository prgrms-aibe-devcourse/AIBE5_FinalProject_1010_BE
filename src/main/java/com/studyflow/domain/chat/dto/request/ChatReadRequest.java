package com.studyflow.domain.chat.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 읽음 처리 요청.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatReadRequest {

    @NotNull(message = "마지막으로 읽은 메시지 ID는 필수입니다.")
    private Long lastReadMessageId;
}