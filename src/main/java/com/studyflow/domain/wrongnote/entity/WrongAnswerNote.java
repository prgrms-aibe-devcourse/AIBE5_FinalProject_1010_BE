package com.studyflow.domain.wrongnote.entity;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;
import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Entity
@Table(name = "wrong_answer_note", indexes = {
        @Index(name = "idx_wrong_note_owner_created", columnList = "owner_id, created_at"),
        @Index(name = "idx_wrong_note_subject", columnList = "subject_id"),
        @Index(name = "idx_wrong_note_source", columnList = "source_type, source_question_id"),
        @Index(name = "idx_wrong_note_practice", columnList = "owner_id, next_review_at, difficulty_score")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WrongAnswerNote extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "question_content", columnDefinition = "TEXT", nullable = false)
    private String questionContent;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String answerContent;

    @Column(columnDefinition = "TEXT")
    private String explanation;

    @Column(name = "wrong_reason", columnDefinition = "TEXT")
    private String wrongReason;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 20)
    private WrongAnswerSourceType sourceType = WrongAnswerSourceType.DIRECT;

    @Column(name = "source_question_id")
    private Long sourceQuestionId;

    @Column(name = "source_answer_id")
    private Long sourceAnswerId;

    @Column(name = "source_title", length = 300)
    private String sourceTitle;

    @Column(name = "review_count", nullable = false, columnDefinition = "int default 0")
    private int reviewCount;

    @Column(name = "correct_count", nullable = false, columnDefinition = "int default 0")
    private int correctCount;

    @Column(name = "incorrect_count", nullable = false, columnDefinition = "int default 0")
    private int incorrectCount;

    @Column(name = "unsure_count", nullable = false, columnDefinition = "int default 0")
    private int unsureCount;

    @Column(name = "answer_view_count", nullable = false, columnDefinition = "int default 0")
    private int answerViewCount;

    @Column(name = "correct_streak", nullable = false, columnDefinition = "int default 0")
    private int correctStreak;

    @Column(name = "difficulty_score", nullable = false, columnDefinition = "int default 0")
    private int difficultyScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_review_result", length = 20)
    private WrongAnswerReviewResult lastReviewResult;

    @Column(name = "last_reviewed_at")
    private LocalDateTime lastReviewedAt;

    @Column(name = "next_review_at")
    private LocalDateTime nextReviewAt;

    @ElementCollection
    @CollectionTable(
            name = "wrong_answer_note_tag",
            joinColumns = @JoinColumn(name = "note_id")
    )
    @Column(name = "tag", length = 50, nullable = false)
    private Set<String> tags = new LinkedHashSet<>();

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static WrongAnswerNote create(
            User owner,
            Subject subject,
            String title,
            String questionContent,
            String answerContent,
            String explanation,
            String wrongReason,
            String memo,
            WrongAnswerSourceType sourceType,
            Long sourceQuestionId,
            Long sourceAnswerId,
            String sourceTitle,
            Collection<String> tags
    ) {
        WrongAnswerNote note = new WrongAnswerNote();
        note.owner = owner;
        note.subject = subject;
        note.title = title;
        note.questionContent = questionContent;
        note.answerContent = answerContent;
        note.explanation = explanation;
        note.wrongReason = wrongReason;
        note.memo = memo;
        note.sourceType = sourceType != null ? sourceType : WrongAnswerSourceType.DIRECT;
        note.sourceQuestionId = sourceQuestionId;
        note.sourceAnswerId = sourceAnswerId;
        note.sourceTitle = sourceTitle;
        note.replaceTags(tags);
        return note;
    }

    public void update(
            Subject subject,
            String title,
            String questionContent,
            String answerContent,
            String explanation,
            String wrongReason,
            String memo,
            Collection<String> tags
    ) {
        this.subject = subject;
        this.title = title;
        this.questionContent = questionContent;
        this.answerContent = answerContent;
        this.explanation = explanation;
        this.wrongReason = wrongReason;
        this.memo = memo;
        replaceTags(tags);
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public void recordReview(WrongAnswerReviewResult result, LocalDateTime reviewedAt) {
        if (result == null) {
            throw new IllegalArgumentException("복습 결과는 필수입니다.");
        }
        LocalDateTime reviewTime = reviewedAt != null ? reviewedAt : LocalDateTime.now();

        switch (result) {
            case ANSWER_VIEWED -> {
                this.answerViewCount++;
                this.difficultyScore = Math.min(100, this.difficultyScore + 1);
                this.nextReviewAt = reviewTime.plusHours(12);
            }
            case CORRECT -> {
                this.reviewCount++;
                this.correctCount++;
                this.correctStreak++;
                this.difficultyScore = Math.max(0, this.difficultyScore - 1 - Math.min(this.correctStreak, 3));
                this.nextReviewAt = reviewTime.plusDays(nextCorrectIntervalDays(this.correctStreak));
            }
            case INCORRECT -> {
                this.reviewCount++;
                this.incorrectCount++;
                this.correctStreak = 0;
                this.difficultyScore = Math.min(100, this.difficultyScore + 3);
                this.nextReviewAt = reviewTime.plusDays(1);
            }
            case UNSURE -> {
                this.reviewCount++;
                this.unsureCount++;
                this.correctStreak = 0;
                this.difficultyScore = Math.min(100, this.difficultyScore + 2);
                this.nextReviewAt = reviewTime.plusDays(2);
            }
        }

        this.lastReviewResult = result;
        this.lastReviewedAt = reviewTime;
    }

    public boolean isOwner(Long userId) {
        return owner != null && owner.getId().equals(userId);
    }

    private void replaceTags(Collection<String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    private static int nextCorrectIntervalDays(int correctStreak) {
        if (correctStreak <= 1) return 3;
        if (correctStreak == 2) return 7;
        if (correctStreak == 3) return 14;
        return 30;
    }
}
