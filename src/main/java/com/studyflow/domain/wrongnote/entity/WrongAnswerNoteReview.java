package com.studyflow.domain.wrongnote.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "wrong_answer_note_review", indexes = {
        @Index(name = "idx_wrong_note_review_note", columnList = "note_id, reviewed_at"),
        @Index(name = "idx_wrong_note_review_reviewer", columnList = "reviewer_id, reviewed_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WrongAnswerNoteReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private WrongAnswerNote note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WrongAnswerReviewResult result;

    @Column(columnDefinition = "TEXT")
    private String memo;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    public static WrongAnswerNoteReview create(
            WrongAnswerNote note,
            User reviewer,
            WrongAnswerReviewResult result,
            String memo,
            LocalDateTime reviewedAt
    ) {
        WrongAnswerNoteReview review = new WrongAnswerNoteReview();
        review.note = note;
        review.reviewer = reviewer;
        review.result = result;
        review.memo = memo;
        review.reviewedAt = reviewedAt;
        return review;
    }
}
