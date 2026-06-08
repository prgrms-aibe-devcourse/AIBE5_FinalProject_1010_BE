package com.studyflow.domain.qna.exception;

import com.studyflow.global.exception.ErrorCode;

/**
 * QnA 비즈니스 상태 위배.
 *
 * <p>예: 이미 채택된 질문에 다시 채택 시도(409), 본인 질문에 본인이 답변 등.
 * 구체적 상태 코드는 전달받은 {@link ErrorCode}를 따른다.</p>
 */
public class QnaInvalidStateException extends RuntimeException {
    private final ErrorCode errorCode;

    public QnaInvalidStateException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
