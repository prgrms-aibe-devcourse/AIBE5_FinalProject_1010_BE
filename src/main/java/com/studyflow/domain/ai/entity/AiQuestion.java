package com.studyflow.domain.ai.entity;

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
 * AI 질문 기록 엔티티 (테이블: ai_question)
 *
 * <p>사용자가 과목을 골라 AI에게 질문하면, 질문 내용과 AI가 생성한 답변을 함께 저장한다.
 * 명세(§26)의 ai_question 테이블에 대응한다.</p>
 *
 * <p>첨부 이미지는 0~N장 가능하며, URL을 직접 갖지 않고 중간 테이블
 * {@link AiQuestionAttachment}를 통해 {@link com.studyflow.domain.file.entity.FileAsset}을
 * 참조한다.(채팅 ChatMessage와 동일한 패턴) 1단계에서는 텍스트만 OpenAI로 보내고,
 * 2단계(이미지 vision)·3단계(음성)에서 첨부를 함께 활용하도록 확장한다.</p>
 *
 * <p>생성/수정 시각은 {@link BaseTimeEntity}에서 자동으로 채워진다.
 * (명세의 createdAt = BaseTimeEntity.createdAt)</p>
 */
@Getter
@Entity
@Table(
        name = "ai_question",
        indexes = {
                // "내 질문 기록을 최신순으로" 조회가 잦으므로 user + 생성시각 인덱스를 둔다.
                @Index(name = "idx_ai_question_user_created", columnList = "user_id, created_at")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 질문한 사용자. (학생/선생님 모두 가능 — 명세 권한: User)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * 이 질문이 속한 대화. 같은 대화의 질문들은 한 화면에 묶여 보인다.
     * (기존 데이터 호환을 위해 nullable. 신규 질문은 항상 대화에 소속된다.)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    /**
     * 질문 과목. (수학/영어 등 — subject 테이블 참조)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    /**
     * 사용자가 입력한 질문 본문.
     */
    @Column(name = "question_text", columnDefinition = "TEXT", nullable = false)
    private String questionText;

    /**
     * AI가 생성한 답변 본문.
     */
    @Column(name = "answer_text", columnDefinition = "TEXT", nullable = false)
    private String answerText;

    /**
     * 질문에 첨부된 이미지 목록. (0~N장)
     *
     * <p>URL 문자열을 직접 들고 있지 않고, 중간 테이블 {@link AiQuestionAttachment}를 통해
     * {@link com.studyflow.domain.file.entity.FileAsset}을 참조한다. 이렇게 하면 한 질문에
     * 이미지를 여러 장 붙일 수 있고, 업로더·크기·삭제여부 등 메타데이터를 함께 관리하며,
     * 저장소(LOCAL/S3) 전환도 FileAsset 안에서 흡수된다.
     * (채팅 ChatMessage ↔ ChatMessageAttachment와 동일한 패턴)</p>
     */
    @OneToMany(mappedBy = "aiQuestion", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiQuestionAttachment> attachments = new ArrayList<>();

    /**
     * 질문 + 답변을 담아 새 기록을 만드는 정적 팩토리.
     *
     * <p>서비스에서 OpenAI 호출로 answerText를 먼저 받은 뒤 이 메서드로 엔티티를 만들고,
     * 이미지는 {@link AiQuestionAttachment#create}로 따로 붙인다.(attachments가 채워짐)</p>
     */
    public static AiQuestion create(
            User user,
            Subject subject,
            Conversation conversation,
            String questionText,
            String answerText
    ) {
        AiQuestion q = new AiQuestion();
        q.user = user;
        q.subject = subject;
        q.conversation = conversation;
        q.questionText = questionText;
        q.answerText = answerText;
        return q;
    }

    /** 기존(대화 미지정) 질문을 사후에 대화에 편입할 때 사용한다. (backfill 전용) */
    public void assignConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    /** 첨부 이미지를 목록에 추가한다. (AiQuestionAttachment.create에서 호출) */
    public void addAttachment(AiQuestionAttachment attachment) {
        this.attachments.add(attachment);
    }
}
