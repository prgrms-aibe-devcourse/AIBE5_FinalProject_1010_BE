package com.studyflow.domain.qna.repository;

import java.util.List;

/** QueryDSL 기반 좋아요 조회. */
public interface QnaAnswerLikeRepositoryCustom {

    /** 질문 상세에서 현재 사용자가 좋아요한 답변 id 집합을 한 번에 조회 (liked 플래그 계산용). */
    List<Long> findLikedAnswerIds(Long userId, List<Long> answerIds);
}
