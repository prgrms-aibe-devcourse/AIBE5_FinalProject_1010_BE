package com.studyflow.domain.wrongnote.dto.response;

import com.studyflow.domain.wrongnote.entity.WrongAnswerNoteReview;
import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;

import java.time.LocalDateTime;

public record WrongAnswerNoteReviewResponse(
        Long id,
        Long noteId,
        WrongAnswerReviewResult result,
        String memo,
        LocalDateTime reviewedAt,
        LocalDateTime createdAt
) {
    public static WrongAnswerNoteReviewResponse from(WrongAnswerNoteReview review) {
        return new WrongAnswerNoteReviewResponse(
                review.getId(),
                review.getNote() != null ? review.getNote().getId() : null,
                review.getResult(),
                review.getMemo(),
                review.getReviewedAt(),
                review.getCreatedAt()
        );
    }
}
