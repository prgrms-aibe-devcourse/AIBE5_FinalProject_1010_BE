package com.studyflow.domain.ai.exception;

// AI 도메인에서는 subject 도메인 예외를 재사용 — 도메인 경계 통일
public class SubjectNotFoundException extends com.studyflow.domain.subject.exception.SubjectNotFoundException {
    public SubjectNotFoundException(Long subjectId) {
        super(subjectId);
    }
}
