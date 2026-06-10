package com.studyflow.domain.qna.dto.response;

/**
 * 질문게시판 전체 통계 (목록 페이지 상단 카드용). 필터와 무관한 전역 집계다.
 * (GET /api/v1/qna/questions/stats)
 */
public record QnaBoardStatsResponse(
        long totalQuestions,
        long resolvedQuestions,
        long waitingQuestions,
        long totalAnswers
) {
    public static QnaBoardStatsResponse of(long total, long resolved, long totalAnswers) {
        return new QnaBoardStatsResponse(total, resolved, Math.max(0, total - resolved), totalAnswers);
    }
}
