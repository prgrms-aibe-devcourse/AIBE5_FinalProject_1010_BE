package com.studyflow.domain.ai.dto.response;

import com.studyflow.domain.ai.entity.AiQuestion;

import java.time.LocalDateTime;

/**
 * AI 질문 기록 조회 응답 항목. (GET /api/v1/ai/questions)
 *
 * <p>명세(§26-2)의 data[] 항목과 동일하게, 과목을 중첩 객체로 내려준다.</p>
 * <pre>
 * {
 *   "aiQuestionId": 1,
 *   "subject": { "subjectId": 2, "name": "미적분" },
 *   "questionText": "...",
 *   "createdAt": "2026-05-22T16:00:00"
 * }
 * </pre>
 * 목록 조회이므로 answerText는 포함하지 않는다(상세 조회/대화 복원 시 별도 처리).
 */
public record AiQuestionHistoryResponse(
        Long aiQuestionId,
        SubjectSummary subject,
        String questionText,
        LocalDateTime createdAt
) {
    /** 응답에 중첩되는 과목 요약 정보. */
    public record SubjectSummary(Long subjectId, String name) {}

    /** 엔티티 → 기록 응답 DTO 변환. */
    public static AiQuestionHistoryResponse from(AiQuestion q) {
        return new AiQuestionHistoryResponse(
                q.getId(),
                new SubjectSummary(q.getSubject().getId(), q.getSubject().getName()),
                q.getQuestionText(),
                q.getCreatedAt()
        );
    }
}
