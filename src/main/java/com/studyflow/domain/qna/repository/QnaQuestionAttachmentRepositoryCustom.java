package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestionAttachment;

import java.util.List;

/** QueryDSL 기반 질문 첨부 조회. */
public interface QnaQuestionAttachmentRepositoryCustom {

    /** 질문 상세의 첨부 이미지를 순서대로 조회. fileAsset(접근 URL)을 함께 fetch. */
    List<QnaQuestionAttachment> findByQuestionIdWithFile(Long questionId);
}
