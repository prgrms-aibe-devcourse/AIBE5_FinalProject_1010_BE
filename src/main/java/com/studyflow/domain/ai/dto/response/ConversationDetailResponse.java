package com.studyflow.domain.ai.dto.response;

import java.util.List;

/**
 * 대화 상세. (GET /api/v1/ai/conversations/{id})
 *
 * <p>대화에 속한 질문·답변 전체를 시간순으로 담아, 클라이언트가 한 대화를 통째로 복원할 수 있게 한다.</p>
 *
 * @param conversationId 대화 id
 * @param title          대화 제목
 * @param subjectId      과목 id
 * @param questions      질문·답변 목록(오래된 것 → 최신)
 */
public record ConversationDetailResponse(
        Long conversationId,
        String title,
        Long subjectId,
        List<AiQuestionResponse> questions
) {
}
