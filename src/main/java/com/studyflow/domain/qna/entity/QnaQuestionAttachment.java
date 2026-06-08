package com.studyflow.domain.qna.entity;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 질문 첨부 이미지. 질문 1건에 여러 장을 붙일 수 있어 {@link FileAsset}을 N:1로 참조한다.
 * (단일 imageUrl 대신 AI 질문(AiQuestionAttachment)과 동일한 컨벤션)
 */
@Getter
@Entity
@Table(name = "qna_question_attachment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_qna_question_attachment_question_file",
                        columnNames = {"question_id", "file_id"})
        },
        indexes = {
                @Index(name = "idx_qna_question_attachment_question", columnList = "question_id"),
                @Index(name = "idx_qna_question_attachment_file", columnList = "file_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaQuestionAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QnaQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileAsset fileAsset;

    // 첨부 순서 (요청 fileIds 순서를 그대로 보존)
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public static QnaQuestionAttachment create(QnaQuestion question, FileAsset fileAsset, int sortOrder) {
        QnaQuestionAttachment attachment = new QnaQuestionAttachment();
        attachment.question = question;
        attachment.fileAsset = fileAsset;
        attachment.sortOrder = sortOrder;
        question.addAttachment(attachment);
        return attachment;
    }
}
