package com.studyflow.domain.qna.dto.response;

/** 답변 좋아요 토글 결과. */
public record QnaLikeResponse(
        Long answerId,
        boolean liked,
        int likeCount
) {
}
