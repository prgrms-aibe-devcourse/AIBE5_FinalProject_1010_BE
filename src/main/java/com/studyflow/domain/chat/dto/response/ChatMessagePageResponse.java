package com.studyflow.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 채팅 메시지 목록 커서 페이징 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagePageResponse {

    private List<ChatMessageResponse> messages;

    private Long nextCursor;

    private boolean hasNext;
}