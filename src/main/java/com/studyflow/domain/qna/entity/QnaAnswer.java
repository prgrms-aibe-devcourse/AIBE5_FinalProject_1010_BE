package com.studyflow.domain.qna.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * QnA 답변. 작성은 TEACHER 권한만 가능하다.
 *
 * <p>{@code likeCount}는 좋아요(QnaAnswerLike) 개수의 비정규화 캐시로, 정렬/표시 성능을 위해 유지한다.
 * 좋아요 토글 시 {@link QnaAnswerLike} 행과 함께 갱신된다.</p>
 */
@Getter
@Entity
@Table(name = "qna_answer", indexes = {
        @Index(name = "idx_qna_answer_question", columnList = "question_id"),
        @Index(name = "idx_qna_answer_author", columnList = "author_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QnaQuestion question;

    // 답변 작성자 (선생님)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 글·이미지를 자유롭게 배치한 본문 블록의 JSON(선택). null이면 평문(content)+첨부(imageUrls)로 렌더(레거시).
    @Column(name = "content_json", columnDefinition = "LONGTEXT")
    private String contentJson;

    @Column(nullable = false)
    private boolean accepted = false;

    @Column(name = "like_count", nullable = false)
    private int likeCount = 0;

    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QnaAnswerLike> likes = new ArrayList<>();

    // 첨부 이미지(여러 장).
    @OneToMany(mappedBy = "answer", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QnaAnswerAttachment> attachments = new ArrayList<>();

    public static QnaAnswer create(QnaQuestion question, User author, String content) {
        QnaAnswer a = new QnaAnswer();
        a.question = question;
        a.author = author;
        a.content = content;
        return a;
    }

    public void addAttachment(QnaAnswerAttachment attachment) {
        this.attachments.add(attachment);
    }

    public void clearAttachments() {
        this.attachments.clear();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    /** 본문 블록 JSON을 설정한다(블록 에디터로 작성/수정 시). 블록 없이 평문이면 null. */
    public void applyContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public void accept() {
        this.accepted = true;
    }

    public void increaseLikeCount() {
        this.likeCount++;
    }

    public void decreaseLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public boolean isAuthor(Long userId) {
        return author != null && author.getId().equals(userId);
    }
}
