package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

/** QueryDSL 기반 질문 조회. */
public interface QnaQuestionRepositoryCustom {

    /**
     * 질문 목록 조회 (필터: 과목 / 검색어(제목·내용) / 해결여부). null 파라미터는 조건에서 제외된다.
     * 목록 카드에 필요한 subject·author는 N+1 방지를 위해 함께 fetch 한다.
     */
    Page<QnaQuestion> findFiltered(Long subjectId, String keyword, Boolean resolved, Pageable pageable);

    /**
     * 위와 동일한 필터에 '답변 많은순' 정렬을 적용해 페이지 조회한다.
     * answerCount는 컬럼이 아니라 집계값이라 Pageable 정렬로 처리할 수 없어 별도 쿼리로 분리한다.
     * (동점은 최신순으로 보조 정렬)
     */
    Page<QnaQuestion> findFilteredOrderByAnswerCount(Long subjectId, String keyword, Boolean resolved, Pageable pageable);

    /** 상세 조회 — subject·author 함께 fetch. */
    Optional<QnaQuestion> findDetailById(Long id);
}
