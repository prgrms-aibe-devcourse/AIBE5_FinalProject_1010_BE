package com.studyflow.domain.subject.exception;

// 존재하지 않는 과목 ID로 조회할 때 발생 → 404
public class SubjectNotFoundException extends RuntimeException {
    public SubjectNotFoundException(Long subjectId) {
        super("존재하지 않는 과목입니다. (id: " + subjectId + ")");
    }
}
