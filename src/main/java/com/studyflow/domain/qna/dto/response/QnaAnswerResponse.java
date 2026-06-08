package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.qna.entity.QnaAnswer;

import java.time.LocalDateTime;
import java.util.List;

/** 질문 상세에 포함되는 답변 항목. */
public record QnaAnswerResponse(
        Long answerId,
        Long authorId,
        String authorName,
        String content,
        boolean isAccepted,
        int likeCount,
        boolean liked,
        List<String> imageUrls,
        LocalDateTime createdAt
) {
    /**
     * @param liked     현재 로그인 사용자가 이 답변에 좋아요했는지 (비로그인이면 false)
     * @param imageUrls 이 답변의 첨부 이미지 URL 목록 (sortOrder 순, 없으면 빈 배열)
     */
    public static QnaAnswerResponse of(QnaAnswer a, boolean liked, List<String> imageUrls) {
        return new QnaAnswerResponse(
                a.getId(),
                a.getAuthor().getId(),
                a.getAuthor().getName(),
                a.getContent(),
                a.isAccepted(),
                a.getLikeCount(),
                liked,
                imageUrls,
                a.getCreatedAt()
        );
    }
}
