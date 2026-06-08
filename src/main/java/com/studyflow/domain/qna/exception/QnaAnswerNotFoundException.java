package com.studyflow.domain.qna.exception;

// 존재하지 않는 답변 조회/수정/삭제/채택 시도 → 404
public class QnaAnswerNotFoundException extends RuntimeException {
    public QnaAnswerNotFoundException(Long answerId) {
        super("답변을 찾을 수 없습니다. (id: " + answerId + ")");
    }
}
