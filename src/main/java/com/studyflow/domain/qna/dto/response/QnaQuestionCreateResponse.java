package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.qna.entity.QnaQuestion;

import java.time.LocalDateTime;

/** 질문 작성 결과. */
public record QnaQuestionCreateResponse(
        Long questionId,
        boolean isResolved,
        LocalDateTime createdAt
) {
    public static QnaQuestionCreateResponse from(QnaQuestion q) {
        return new QnaQuestionCreateResponse(q.getId(), q.isResolved(), q.getCreatedAt());
    }
}
