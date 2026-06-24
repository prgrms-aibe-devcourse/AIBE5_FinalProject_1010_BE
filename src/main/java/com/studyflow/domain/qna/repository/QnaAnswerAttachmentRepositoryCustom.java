package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswerAttachment;

import java.util.List;

/** QueryDSL 기반 답변 첨부 조회. */
public interface QnaAnswerAttachmentRepositoryCustom {

    /** 질문 상세의 여러 답변에 달린 첨부 이미지를 한 번에 조회 (N+1 방지). fileAsset 함께 fetch. */
    List<QnaAnswerAttachment> findByAnswerIdsWithFile(List<Long> answerIds);
}
