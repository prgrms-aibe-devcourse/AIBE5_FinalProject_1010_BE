package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long>, QnaQuestionRepositoryCustom {
    // 조회 쿼리는 QnaQuestionRepositoryCustom(QueryDSL)에 정의되어 있다.

    /** 해결 여부별 질문 수 (게시판 통계용). */
    long countByResolved(boolean resolved);
}
