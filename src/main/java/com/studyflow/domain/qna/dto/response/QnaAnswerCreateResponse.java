package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.qna.entity.QnaAnswer;

import java.time.LocalDateTime;

/** 답변 작성 결과. */
public record QnaAnswerCreateResponse(
        Long answerId,
        Long questionId,
        Long authorId,
        LocalDateTime createdAt
) {
    public static QnaAnswerCreateResponse from(QnaAnswer a) {
        return new QnaAnswerCreateResponse(
                a.getId(),
                a.getQuestion().getId(),
                a.getAuthor().getId(),
                a.getCreatedAt()
        );
    }
}
