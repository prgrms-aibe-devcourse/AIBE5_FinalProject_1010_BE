package com.studyflow.domain.ai.entity;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * AI 질문 ↔ 첨부 이미지(FileAsset) 연결 테이블. (테이블: ai_question_attachment)
 *
 * <p>한 질문에 이미지를 여러 장 붙일 수 있도록, AiQuestion과 FileAsset 사이를 잇는
 * 중간 엔티티다. 채팅의 {@code ChatMessageAttachment}와 동일한 패턴이다.</p>
 *
 * <p>OpenAI vision은 한 질문에 이미지 여러 장을 받을 수 있으므로(예: 여러 페이지로 찍은 문제,
 * 문제 사진 + 본인 풀이), 단일 FK 대신 이 1:N 구조를 사용한다.</p>
 */
@Getter
@Entity
@Table(
        name = "ai_question_attachment",
        uniqueConstraints = {
                // 같은 질문에 같은 파일을 중복으로 붙이지 못하게 막는다.
                @UniqueConstraint(
                        name = "uk_ai_question_attachment_question_file",
                        columnNames = {"ai_question_id", "file_id"}
                )
        },
        indexes = {
                @Index(name = "idx_ai_question_attachment_question", columnList = "ai_question_id"),
                @Index(name = "idx_ai_question_attachment_file", columnList = "file_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiQuestionAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 첨부가 속한 질문.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ai_question_id", nullable = false)
    private AiQuestion aiQuestion;

    /**
     * 실제 파일 메타데이터. (저장소 LOCAL/S3를 FileAsset이 흡수)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileAsset fileAsset;

    /**
     * 한 질문에 이미지가 여러 장일 때 표시/전달 순서.
     */
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    /**
     * 첨부를 만들고 질문의 attachments 목록에 연결한다.
     *
     * <p>양방향 연관을 한 번에 맞추기 위해 {@link AiQuestion#addAttachment}를 호출한다.</p>
     */
    public static AiQuestionAttachment create(
            AiQuestion aiQuestion,
            FileAsset fileAsset,
            int sortOrder
    ) {
        AiQuestionAttachment attachment = new AiQuestionAttachment();
        attachment.aiQuestion = aiQuestion;
        attachment.fileAsset = fileAsset;
        attachment.sortOrder = sortOrder;

        aiQuestion.addAttachment(attachment);

        return attachment;
    }
}
