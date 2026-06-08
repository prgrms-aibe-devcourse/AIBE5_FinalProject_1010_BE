package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaQuestionAttachmentRepository
        extends JpaRepository<QnaQuestionAttachment, Long>, QnaQuestionAttachmentRepositoryCustom {
    // 조회 쿼리는 QnaQuestionAttachmentRepositoryCustom(QueryDSL)에 정의되어 있다.
}
