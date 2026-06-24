package com.studyflow.domain.wrongnote.dto.response;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;
import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;
import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;

import java.time.LocalDateTime;
import java.util.List;

public record WrongAnswerNoteResponse(
        Long id,
        Long ownerId,
        String ownerName,
        Long subjectId,
        String subjectName,
        String title,
        String questionContent,
        String answerContent,
        String explanation,
        String wrongReason,
        String memo,
        List<String> tags,
        WrongAnswerSourceType sourceType,
        Long sourceQuestionId,
        Long sourceAnswerId,
        String sourceTitle,
        int reviewCount,
        int correctCount,
        int incorrectCount,
        int unsureCount,
        int answerViewCount,
        int correctStreak,
        int difficultyScore,
        WrongAnswerReviewResult lastReviewResult,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static WrongAnswerNoteResponse from(WrongAnswerNote note) {
        User owner = note.getOwner();
        Subject subject = note.getSubject();
        return new WrongAnswerNoteResponse(
                note.getId(),
                owner != null ? owner.getId() : null,
                owner != null ? owner.getName() : null,
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                note.getTitle(),
                note.getQuestionContent(),
                note.getAnswerContent(),
                note.getExplanation(),
                note.getWrongReason(),
                note.getMemo(),
                List.copyOf(note.getTags()),
                note.getSourceType(),
                note.getSourceQuestionId(),
                note.getSourceAnswerId(),
                note.getSourceTitle(),
                note.getReviewCount(),
                note.getCorrectCount(),
                note.getIncorrectCount(),
                note.getUnsureCount(),
                note.getAnswerViewCount(),
                note.getCorrectStreak(),
                note.getDifficultyScore(),
                note.getLastReviewResult(),
                note.getLastReviewedAt(),
                note.getNextReviewAt(),
                note.getCreatedAt(),
                note.getUpdatedAt()
        );
    }
}
