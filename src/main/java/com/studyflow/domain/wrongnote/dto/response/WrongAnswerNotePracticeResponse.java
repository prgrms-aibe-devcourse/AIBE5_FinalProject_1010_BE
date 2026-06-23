package com.studyflow.domain.wrongnote.dto.response;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;
import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;

import java.time.LocalDateTime;
import java.util.List;

public record WrongAnswerNotePracticeResponse(
        Long id,
        Long subjectId,
        String subjectName,
        String title,
        String questionContent,
        List<String> tags,
        WrongAnswerSourceType sourceType,
        int reviewCount,
        int correctCount,
        int incorrectCount,
        int unsureCount,
        int answerViewCount,
        int correctStreak,
        int difficultyScore,
        LocalDateTime lastReviewedAt,
        LocalDateTime nextReviewAt,
        String recommendReason
) {
    public static WrongAnswerNotePracticeResponse from(WrongAnswerNote note, LocalDateTime now) {
        Subject subject = note.getSubject();
        return new WrongAnswerNotePracticeResponse(
                note.getId(),
                subject != null ? subject.getId() : null,
                subject != null ? subject.getName() : null,
                note.getTitle(),
                note.getQuestionContent(),
                List.copyOf(note.getTags()),
                note.getSourceType(),
                note.getReviewCount(),
                note.getCorrectCount(),
                note.getIncorrectCount(),
                note.getUnsureCount(),
                note.getAnswerViewCount(),
                note.getCorrectStreak(),
                note.getDifficultyScore(),
                note.getLastReviewedAt(),
                note.getNextReviewAt(),
                recommendReason(note, now)
        );
    }

    private static String recommendReason(WrongAnswerNote note, LocalDateTime now) {
        if (note.getLastReviewedAt() == null) {
            return "아직 복습 기록이 없어 먼저 풀어보면 좋은 문제입니다.";
        }
        if (note.getNextReviewAt() != null && !note.getNextReviewAt().isAfter(now)) {
            return "복습 예정 시간이 지나 다시 확인할 차례입니다.";
        }
        if (note.getDifficultyScore() >= 5) {
            return "틀림 또는 헷갈림 기록이 많아 다시 풀어보면 좋습니다.";
        }
        return "마지막 복습 이후 시간이 지나 기억 확인용으로 추천합니다.";
    }
}
