package com.studyflow.domain.qna.exception;

import com.studyflow.global.exception.ErrorCode;

/**
 * QnA 권한 위배 → 403.
 *
 * <p>예: 본인이 작성하지 않은 질문/답변/댓글 수정·삭제, 질문 작성자가 아닌 사용자의 답변 채택 등.</p>
 */
public class QnaForbiddenException extends RuntimeException {
    private final ErrorCode errorCode;

    public QnaForbiddenException(String message) {
        super(message);
        this.errorCode = ErrorCode.QNA_FORBIDDEN;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
