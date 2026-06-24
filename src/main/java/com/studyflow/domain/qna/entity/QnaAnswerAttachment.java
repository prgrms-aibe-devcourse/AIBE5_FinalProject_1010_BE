package com.studyflow.domain.qna.entity;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 답변 첨부 이미지. 답변 1건에 여러 장을 붙일 수 있어 {@link FileAsset}을 N:1로 참조한다.
 */
@Getter
@Entity
@Table(name = "qna_answer_attachment",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_qna_answer_attachment_answer_file",
                        columnNames = {"answer_id", "file_id"})
        },
        indexes = {
                @Index(name = "idx_qna_answer_attachment_answer", columnList = "answer_id"),
                @Index(name = "idx_qna_answer_attachment_file", columnList = "file_id")
        })
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QnaAnswerAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answer_id", nullable = false)
    private QnaAnswer answer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileAsset fileAsset;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public static QnaAnswerAttachment create(QnaAnswer answer, FileAsset fileAsset, int sortOrder) {
        QnaAnswerAttachment attachment = new QnaAnswerAttachment();
        attachment.answer = answer;
        attachment.fileAsset = fileAsset;
        attachment.sortOrder = sortOrder;
        answer.addAttachment(attachment);
        return attachment;
    }
}
