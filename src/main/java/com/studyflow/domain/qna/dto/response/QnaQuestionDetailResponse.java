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
        List<QnaImageResponse> images,
        List<QnaBlockResponse> blocks,
        boolean isResolved,
        int viewCount,
        AuthorResponse author,
        List<QnaAnswerResponse> answers,
        LocalDateTime createdAt
) {
    public record AuthorResponse(Long userId, String name) {
    }

    /**
     * @param images  질문 첨부 이미지 목록 (sortOrder 순, 없으면 빈 배열). 각 항목은 fileId+url을 가져
     *                수정 화면에서 일부만 남기고 일부만 삭제할 수 있다.
     * @param blocks  글·이미지를 배치한 본문 블록(순서대로). 블록 에디터로 작성된 글이 아니면 null
     *                → 이때 FE는 content+images로 폴백 렌더한다.
     * @param answers 답변 목록
     */
    public static QnaQuestionDetailResponse of(QnaQuestion q, List<QnaImageResponse> images,
                                               List<QnaBlockResponse> blocks, List<QnaAnswerResponse> answers) {
        return new QnaQuestionDetailResponse(
                q.getId(),
                QnaSubjectResponse.from(q.getSubject()),
                q.getTitle(),
                q.getContent(),
                images,
                blocks,
                q.isResolved(),
                q.getViewCount(),
                new AuthorResponse(q.getAuthor().getId(), q.getAuthor().getName()),
                answers,
                q.getCreatedAt()
        );
    }
}
