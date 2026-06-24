package com.studyflow.domain.qna.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 답변 좋아요. (answer_id, user_id) 유니크 제약으로 한 사용자가 같은 답변에 중복 좋아요할 수 없다.
 */
@Getter
@Entity
@Table(name = "qna_answer_like",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_qna_answer_like_answer_user", columnNames = {"answer_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_qna_answer_like_answer", columnList = "answer_id"),
                @Index(name = "idx_qna_answer_like_user", columnList = "user_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswerLike extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private QnaAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static QnaAnswerLike create(QnaAnswer answer, User user) {
        QnaAnswerLike like = new QnaAnswerLike();
        like.answer = answer;
        like.user = user;
        return like;
    }
}
