package com.studyflow.domain.qna.dto.response;

import com.studyflow.domain.subject.entity.Subject;

/** QnA 응답에 포함되는 과목 요약. */
public record QnaSubjectResponse(
        Long subjectId,
        String name
) {
    public static QnaSubjectResponse from(Subject subject) {
        return new QnaSubjectResponse(subject.getId(), subject.getName());
    }
}
