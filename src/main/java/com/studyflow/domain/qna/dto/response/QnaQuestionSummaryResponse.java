package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.qna.entity.QnaQuestion;

import java.time.LocalDateTime;

/** 질문 목록 항목. (GET /api/v1/qna/questions) */
public record QnaQuestionSummaryResponse(
        Long questionId,
        String title,
        QnaSubjectResponse subject,
        Long authorId,
        String authorName,
        boolean isResolved,
        long answerCount,
        int viewCount,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
    /**
     * @param thumbnailUrl 첫 번째 첨부 이미지 URL(목록 카드 썸네일용). 이미지가 없으면 null.
     */
    public static QnaQuestionSummaryResponse of(QnaQuestion q, long answerCount, String thumbnailUrl) {
        return new QnaQuestionSummaryResponse(
                q.getId(),
                q.getTitle(),
                QnaSubjectResponse.from(q.getSubject()),
                q.getAuthor().getId(),
                q.getAuthor().getName(),
                q.isResolved(),
                answerCount,
                q.getViewCount(),
                thumbnailUrl,
                q.getCreatedAt()
        );
    }
}
