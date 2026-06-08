package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.qna.entity.QnaQuestion;

import java.time.LocalDateTime;
import java.util.List;

/** 질문 상세. (GET /api/v1/qna/questions/{questionId}) */
public record QnaQuestionDetailResponse(
        Long questionId,
        QnaSubjectResponse subject,
        String title,
        String content,
        List<String> imageUrls,
        boolean isResolved,
        int viewCount,
        AuthorResponse author,
        List<QnaAnswerResponse> answers,
        LocalDateTime createdAt
) {
    public record AuthorResponse(Long userId, String name) {
    }

    /**
     * @param imageUrls 질문 첨부 이미지 URL 목록 (sortOrder 순, 없으면 빈 배열)
     * @param answers   답변 목록
     */
    public static QnaQuestionDetailResponse of(QnaQuestion q, List<String> imageUrls, List<QnaAnswerResponse> answers) {
        return new QnaQuestionDetailResponse(
                q.getId(),
                QnaSubjectResponse.from(q.getSubject()),
                q.getTitle(),
                q.getContent(),
                imageUrls,
                q.isResolved(),
                q.getViewCount(),
                new AuthorResponse(q.getAuthor().getId(), q.getAuthor().getName()),
                answers,
                q.getCreatedAt()
        );
    }
}
