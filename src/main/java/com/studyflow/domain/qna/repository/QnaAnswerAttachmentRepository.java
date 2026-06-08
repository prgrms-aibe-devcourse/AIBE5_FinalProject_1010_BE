package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaAnswerAttachmentRepository
        extends JpaRepository<QnaAnswerAttachment, Long>, QnaAnswerAttachmentRepositoryCustom {
    // 조회 쿼리는 QnaAnswerAttachmentRepositoryCustom(QueryDSL)에 정의되어 있다.
}
