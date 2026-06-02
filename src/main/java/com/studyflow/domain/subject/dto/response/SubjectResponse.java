package com.studyflow.domain.subject.dto.response;

import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;

/**
 * 과목 목록 응답 항목. (GET /api/v1/subjects)
 *
 * <p>FE는 이 목록으로 과목 선택 UI(AI 질문 페이지의 과목 칩 등)를 그리고,
 * 사용자가 고른 과목의 {@code subjectId}를 AI 질문 요청에 담아 보낸다.</p>
 *
 * @param subjectId 과목 id
 * @param name      과목 한글 이름 (예: "수학")
 * @param category  대분류 코드 (예: "MATH"). 최상위 8개 과목이 아니면 null일 수 있다.
 */
public record SubjectResponse(
        Long subjectId,
        String name,
        String category
) {
    public static SubjectResponse from(Subject subject) {
        SubjectCategory category = subject.getCategory();
        return new SubjectResponse(
                subject.getId(),
                subject.getName(),
                category == null ? null : category.name()
        );
    }
}
