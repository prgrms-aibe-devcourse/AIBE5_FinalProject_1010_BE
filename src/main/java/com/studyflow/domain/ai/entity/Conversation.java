package com.studyflow.domain.ai.entity;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 대화(Conversation) — 여러 개의 {@link AiQuestion}(질문+답변)을 하나로 묶는 단위.
 * (ChatGPT의 한 "대화"에 해당. 사이드바에는 대화 단위로 타이틀이 노출된다.)
 *
 * <p>대화는 한 과목에 소속되며, 타이틀은 그 대화의 첫 질문에서 만든다.</p>
 */
@Entity
@Table(
        name = "conversation",
        indexes = {
                // "내 대화 목록(과목별, 최신순)" 조회가 잦으므로 인덱스를 둔다.
                @Index(name = "idx_conversation_user_subject", columnList = "user_id, subject_id, created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Conversation extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대화 소유자. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 대화가 속한 과목. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /** 사이드바에 노출되는 대화 제목(첫 질문에서 생성). */
    @Column(length = 120, nullable = false)
    private String title;

    /**
     * 첫 질문 텍스트로부터 대화를 생성한다. 제목은 첫 질문을 짧게 잘라 만든다.
     */
    public static Conversation createFromFirstQuestion(User user, Subject subject, String firstQuestion) {
        Conversation conversation = new Conversation();
        conversation.user = user;
        conversation.subject = subject;
        conversation.title = toTitle(firstQuestion);
        return conversation;
    }

    /** 질문 텍스트를 제목용으로 다듬는다(공백 정리 + 30자 제한). */
    private static String toTitle(String question) {
        String trimmed = (question == null) ? "" : question.strip();
        if (trimmed.isEmpty()) {
            return "새 대화";
        }
        return trimmed.length() <= 30 ? trimmed : trimmed.substring(0, 30) + "…";
    }
}
