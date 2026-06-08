package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestionAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QnaQuestionAttachmentRepository extends JpaRepository<QnaQuestionAttachment, Long> {

    // 질문 상세의 첨부 이미지를 순서대로 조회. fileAsset(접근 URL)을 함께 fetch.
    @Query("SELECT a FROM QnaQuestionAttachment a " +
            "JOIN FETCH a.fileAsset " +
            "WHERE a.question.id = :questionId " +
            "ORDER BY a.sortOrder ASC")
    List<QnaQuestionAttachment> findByQuestionIdWithFile(@Param("questionId") Long questionId);
}
