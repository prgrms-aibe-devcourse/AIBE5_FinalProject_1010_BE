package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswerAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QnaAnswerAttachmentRepository extends JpaRepository<QnaAnswerAttachment, Long> {

    // 질문 상세의 여러 답변에 달린 첨부 이미지를 한 번에 조회 (N+1 방지). fileAsset 함께 fetch.
    @Query("SELECT a FROM QnaAnswerAttachment a " +
            "JOIN FETCH a.fileAsset " +
            "WHERE a.answer.id IN :answerIds " +
            "ORDER BY a.sortOrder ASC")
    List<QnaAnswerAttachment> findByAnswerIdsWithFile(@Param("answerIds") List<Long> answerIds);
}
