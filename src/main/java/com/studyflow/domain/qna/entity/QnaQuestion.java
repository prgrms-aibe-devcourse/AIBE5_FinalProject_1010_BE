package com.studyflow.domain.qna.entity;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * QnA 질문 (지식인 스타일 질문게시판).
 *
 * <p>작성은 STUDENT 권한만 가능하다(보안 규칙 + 서비스에서 재확인).
 * 답변이 채택되면 {@code resolved = true}가 되고, 채택된 답변의 작성 선생님은 내공 점수를 받는다.</p>
 */
@Getter
@Entity
@Table(name = "qna_question", indexes = {
        @Index(name = "idx_qna_question_subject", columnList = "subject_id"),
        @Index(name = "idx_qna_question_author", columnList = "author_id"),
        @Index(name = "idx_qna_question_resolved_created", columnList = "resolved, created_at")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 질문 작성자 (학생)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 글·이미지를 자유롭게 배치한 본문 블록의 JSON(선택). null이면 평문(content)+첨부(images)로 렌더(레거시).
    @Column(name = "content_json", columnDefinition = "LONGTEXT")
    private String contentJson;

    // 답변 채택 여부 (= 해결됨)
    @Column(nullable = false)
    private boolean resolved = false;

    // 이 질문에 대한 ANSWER_ACCEPTED 내공이 이미 지급되었는지 여부.
    // 질문 하나당 내공은 누가 받았는지와 관계없이 1회만 지급한다.
    // 즉, 선생님 A가 채택 후 자신의 답변을 삭제하고 선생님 B가 재채택되어도 B에게 추가 내공은 지급하지 않는다.
    // (내공 복사 악용 방지 목적의 의도된 트레이드오프)
    @Column(name = "accepted_answer_naegong_paid", nullable = false)
    private boolean acceptedAnswerNaegongPaid = false;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QnaAnswer> answers = new ArrayList<>();

    // 첨부 이미지(여러 장). create()/addAttachment로 추가되며 cascade로 함께 저장·삭제된다.
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QnaQuestionAttachment> attachments = new ArrayList<>();

    public static QnaQuestion create(User author, Subject subject, String title, String content) {
        QnaQuestion q = new QnaQuestion();
        q.author = author;
        q.subject = subject;
        q.title = title;
        q.content = content;
        return q;
    }

    public void addAttachment(QnaQuestionAttachment attachment) {
        this.attachments.add(attachment);
    }

    /** 첨부 이미지를 모두 비운다. (수정 시 이미지 교체용 — orphanRemoval로 기존 행 삭제) */
    public void clearAttachments() {
        this.attachments.clear();
    }

    public void update(Subject subject, String title, String content) {
        this.subject = subject;
        this.title = title;
        this.content = content;
    }

    /** 본문 블록 JSON을 설정한다(블록 에디터로 작성/수정 시). 블록 없이 평문이면 null. */
    public void applyContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public void increaseViewCount() {
        this.viewCount++;
    }

    public void markResolved() {
        this.resolved = true;
    }

    public void markNaegongPaid() {
        this.acceptedAnswerNaegongPaid = true;
    }

    /** 채택된 답변이 삭제되는 경우 등에 해결 상태를 되돌린다. */
    public void unmarkResolved() {
        this.resolved = false;
    }

    public boolean isAuthor(Long userId) {
        return author != null && author.getId().equals(userId);
    }
}
