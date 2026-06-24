package com.studyflow.domain.wrongnote.dto.response;

import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;

import java.time.LocalDateTime;

public record WrongAnswerNoteAnswerResponse(
        Long id,
        String title,
        String answerContent,
        String explanation,
        String wrongReason,
        String memo,
        int answerViewCount,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt
) {
    public static WrongAnswerNoteAnswerResponse from(WrongAnswerNote note) {
        return new WrongAnswerNoteAnswerResponse(
                note.getId(),
                note.getTitle(),
                note.getAnswerContent(),
                note.getExplanation(),
                note.getWrongReason(),
                note.getMemo(),
                note.getAnswerViewCount(),
                note.getLastReviewedAt(),
                note.getNextReviewAt()
        );
    }
}
