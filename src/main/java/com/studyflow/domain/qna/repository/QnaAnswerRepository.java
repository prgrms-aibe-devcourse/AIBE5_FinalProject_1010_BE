package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long>, QnaAnswerRepositoryCustom {

    long countByQuestionId(Long questionId);

    boolean existsByQuestionIdAndAccepted(Long questionId, boolean accepted);

    Optional<QnaAnswer> findFirstByQuestionIdAndAcceptedTrue(Long questionId);

    // 선생님 상세 페이지 활동 통계 — 작성자(author=User) 기준 답변 수 / 채택된 답변 수
    long countByAuthorId(Long authorId);

    long countByAuthorIdAndAcceptedTrue(Long authorId);

    // fetch join/집계 쿼리는 QnaAnswerRepositoryCustom(QueryDSL)에 정의되어 있다.
}
