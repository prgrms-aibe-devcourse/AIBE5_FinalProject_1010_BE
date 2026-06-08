package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long>, QnaAnswerRepositoryCustom {

    long countByQuestionId(Long questionId);

    boolean existsByQuestionIdAndAccepted(Long questionId, boolean accepted);

    // fetch join/집계 쿼리는 QnaAnswerRepositoryCustom(QueryDSL)에 정의되어 있다.
}
