package com.studyflow.domain.qna.exception;

import com.studyflow.global.exception.ErrorCode;

// 존재하지 않는 질문 조회/수정/삭제 시도 → 404
public class QnaQuestionNotFoundException extends RuntimeException {
    private final ErrorCode errorCode;

    public QnaQuestionNotFoundException(Long questionId) {
        super("질문을 찾을 수 없습니다. (id: " + questionId + ")");
        this.errorCode = ErrorCode.QNA_QUESTION_NOT_FOUND;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
