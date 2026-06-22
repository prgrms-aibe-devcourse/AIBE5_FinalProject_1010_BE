package com.studyflow.domain.wrongnote.entity;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.user.entity.User;
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
        @Index(name = "idx_wrong_note_source", columnList = "source_type, source_question_id")
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

    public boolean isOwner(Long userId) {
        return owner != null && owner.getId().equals(userId);
    }

    private void replaceTags(Collection<String> tags) {
        this.tags.clear();
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }
}
