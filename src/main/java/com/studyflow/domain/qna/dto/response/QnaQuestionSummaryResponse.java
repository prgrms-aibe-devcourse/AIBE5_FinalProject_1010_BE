package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.qna.entity.QnaQuestion;

import java.time.LocalDateTime;

/** 질문 목록 항목. (GET /api/v1/qna/questions) */
public record QnaQuestionSummaryResponse(
        Long questionId,
        String title,
        String contentPreview,
        QnaSubjectResponse subject,
        Long authorId,
        String authorName,
        boolean isResolved,
        long answerCount,
        int viewCount,
        String thumbnailUrl,
        LocalDateTime createdAt
) {
    // 목록 카드 본문 미리보기 최대 길이. 실제 말줄임(…)은 프론트의 line-clamp가 처리한다.
    private static final int PREVIEW_MAX = 300;

    /**
     * @param thumbnailUrl 첫 번째 첨부 이미지 URL(목록 카드 썸네일용). 이미지가 없으면 null.
     */
    public static QnaQuestionSummaryResponse of(QnaQuestion q, long answerCount, String thumbnailUrl) {
        return new QnaQuestionSummaryResponse(
                q.getId(),
                q.getTitle(),
                previewOf(q.getContent()),
                QnaSubjectResponse.from(q.getSubject()),
                q.getAuthor().getId(),
                q.getAuthor().getName(),
                q.isResolved(),
                answerCount,
                q.getViewCount(),
                thumbnailUrl,
                q.getCreatedAt()
        );
    }

    /** 본문을 카드 미리보기용으로 줄인다. 줄바꿈은 공백으로 정리하고 최대 길이로 자른다. */
    private static String previewOf(String content) {
        if (content == null) {
            return ""; // content는 not-null이지만 방어적으로 빈 문자열 반환(FE null 가드 불필요)
        }
        String flattened = content.replaceAll("\\s+", " ").trim();
        return flattened.length() > PREVIEW_MAX ? flattened.substring(0, PREVIEW_MAX) : flattened;
    }
}
