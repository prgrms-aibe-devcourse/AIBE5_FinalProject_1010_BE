package com.studyflow.domain.ai.dto.response;

import com.studyflow.domain.ai.entity.Conversation;

import java.time.LocalDateTime;

/**
 * 대화 목록 항목. (GET /api/v1/ai/conversations)
 *
 * <p>사이드바에 대화 단위로 타이틀을 노출하기 위한 요약 정보.</p>
 *
 * @param conversationId 대화 id
 * @param title          대화 제목(첫 질문에서 생성)
 * @param subjectId      대화가 속한 과목 id
 * @param createdAt      생성 시각
 * @param updatedAt      마지막 수정 시각
 */
public record ConversationSummaryResponse(
        Long conversationId,
        String title,
        Long subjectId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ConversationSummaryResponse from(Conversation c) {
        return new ConversationSummaryResponse(
                c.getId(),
                c.getTitle(),
                c.getSubject().getId(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }
}
