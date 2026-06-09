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
        List<QnaImageResponse> images,
        List<QnaBlockResponse> blocks,
        LocalDateTime createdAt
) {
    /**
     * @param liked  현재 로그인 사용자가 이 답변에 좋아요했는지 (비로그인이면 false)
     * @param images 이 답변의 첨부 이미지 목록 (sortOrder 순, 없으면 빈 배열). 질문과 동일하게 fileId+url을 가져
     *               답변 수정 화면에서 일부만 남기고 일부만 삭제할 수 있다.
     * @param blocks 글·이미지를 배치한 본문 블록(순서대로). 블록 에디터로 작성되지 않았으면 null
     *               → 이때 FE는 content+images로 폴백 렌더한다.
     */
    public static QnaAnswerResponse of(QnaAnswer a, boolean liked, List<QnaImageResponse> images, List<QnaBlockResponse> blocks) {
        return new QnaAnswerResponse(
                a.getId(),
                a.getAuthor().getId(),
                a.getAuthor().getName(),
                a.getContent(),
                a.isAccepted(),
                a.getLikeCount(),
                liked,
                images,
                blocks,
                a.getCreatedAt()
        );
    }
}
