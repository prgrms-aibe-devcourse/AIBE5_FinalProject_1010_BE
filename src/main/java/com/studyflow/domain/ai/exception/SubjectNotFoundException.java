package com.studyflow.domain.ai.exception;

/**
 * 존재하지 않는 과목 id로 질문을 시도할 때 발생 → 404.
 */
public class SubjectNotFoundException extends RuntimeException {
    public SubjectNotFoundException(Long subjectId) {
        super("존재하지 않는 과목입니다. (id: " + subjectId + ")");
    }
}
