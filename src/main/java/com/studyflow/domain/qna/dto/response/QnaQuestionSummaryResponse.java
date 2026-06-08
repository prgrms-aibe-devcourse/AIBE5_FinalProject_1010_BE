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
        LocalDateTime createdAt
) {
    public static QnaQuestionSummaryResponse of(QnaQuestion q, long answerCount) {
        return new QnaQuestionSummaryResponse(
                q.getId(),
                q.getTitle(),
                QnaSubjectResponse.from(q.getSubject()),
                q.getAuthor().getId(),
                q.getAuthor().getName(),
                q.isResolved(),
                answerCount,
                q.getViewCount(),
                q.getCreatedAt()
        );
    }
}
