package com.studyflow.domain.ai.exception;

/**
 * 존재하지 않거나 본인 소유가 아닌 AI 질문 기록을 조회할 때 발생 → 404.
 *
 * <p>타인의 기록은 "없는 것"으로 취급해 존재 여부를 노출하지 않는다.</p>
 */
public class AiQuestionNotFoundException extends RuntimeException {
    public AiQuestionNotFoundException(Long aiQuestionId) {
        super("질문 기록을 찾을 수 없습니다. (id: " + aiQuestionId + ")");
    }
}
