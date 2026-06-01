package com.studyflow.domain.ai.exception;

/**
 * AI(외부 LLM) 호출 실패 시 발생하는 예외.
 *
 * <p>OpenAI 호출 중 네트워크 오류·인증 오류·응답 파싱 실패 등이 생기면 던진다.
 * GlobalExceptionHandler에서 502(Bad Gateway)로 변환한다.
 * (우리 서버 잘못이 아니라 외부 의존 서비스 문제라는 의미)</p>
 */
public class AiServiceException extends RuntimeException {
    public AiServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiServiceException(String message) {
        super(message);
    }
}
