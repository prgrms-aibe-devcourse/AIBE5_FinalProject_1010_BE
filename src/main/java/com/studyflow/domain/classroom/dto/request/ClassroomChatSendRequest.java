package com.studyflow.domain.classroom.dto.request;

import com.studyflow.domain.chat.enums.ChatMessageType;

import java.util.List;

/**
 * 강의실 채팅 전송(23-2) 요청 — WebSocket payload.
 *
 * <p>발행 경로: /pub/classroom-sessions/{sessionId}/chats</p>
 * <ul>
 *   <li>TEXT: content 필수, fileIds 비움</li>
 *   <li>IMAGE: fileIds 필수(업로드된 이미지 fileId), content는 캡션 또는 null</li>
 * </ul>
 * messageType이 null이면 TEXT로 간주(하위 호환).
 */
public record ClassroomChatSendRequest(
        ChatMessageType messageType,
        String content,
        List<Long> fileIds
) {
}
