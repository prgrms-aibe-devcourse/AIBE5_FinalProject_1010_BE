package com.studyflow.domain.ai.exception;

/**
 * 존재하지 않거나 본인 소유가 아닌 대화를 조회/이어쓰기할 때 발생 → 404.
 */
public class ConversationNotFoundException extends RuntimeException {
    public ConversationNotFoundException(Long conversationId) {
        super("대화를 찾을 수 없습니다. (id: " + conversationId + ")");
    }
}
