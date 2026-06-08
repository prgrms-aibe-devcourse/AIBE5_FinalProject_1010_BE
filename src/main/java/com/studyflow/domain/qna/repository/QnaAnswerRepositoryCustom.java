package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswer;

import java.util.List;
import java.util.Optional;

/** QueryDSL 기반 답변 조회. */
public interface QnaAnswerRepositoryCustom {

    /** 질문 상세의 답변 목록. 채택 → 좋아요 많은 순 → 오래된 순. 작성자 함께 fetch. */
    List<QnaAnswer> findByQuestionIdWithAuthor(Long questionId);

    /** 채택/답변 작성용 — question·question.author·author 함께 fetch. */
    Optional<QnaAnswer> findDetailById(Long id);

    /** 목록 화면용 — 여러 질문의 답변 개수를 한 번에 집계 (N+1 방지). */
    List<QuestionAnswerCount> countByQuestionIds(List<Long> questionIds);

    /** 질문별 답변 개수 집계 결과. */
    record QuestionAnswerCount(Long questionId, Long cnt) {
    }
}
