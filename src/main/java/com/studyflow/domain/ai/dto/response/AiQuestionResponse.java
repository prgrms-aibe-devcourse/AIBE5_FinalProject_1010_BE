package com.studyflow.domain.ai.dto.response;

import com.studyflow.domain.ai.entity.AiQuestion;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * AI 질문 요청 응답. (POST /api/v1/ai/questions)
 *
 * <p>명세(§26)의 data 객체와 동일한 필드 구성이다.
 * 단, 기존 백엔드 컨벤션에 맞춰 공통 래퍼({success,code,message,data}) 없이
 * 이 DTO를 그대로(raw) 반환한다.</p>
 *
 * @param aiQuestionId     저장된 질문 기록 id
 * @param userId           질문자 id
 * @param subjectId        과목 id
 * @param questionText      질문 본문
 * @param questionImageUrls 첨부 이미지 URL 목록 (없으면 빈 배열). 내부적으로는 FileAsset을 참조하지만,
 *                          클라이언트가 바로 화면에 그릴 수 있도록 응답에는 URL 목록으로 펼쳐서 내려준다.
 * @param answerText        AI 답변 본문
 * @param createdAt         생성 시각
 */
public record AiQuestionResponse(
        Long aiQuestionId,
        Long userId,
        Long subjectId,
        String questionText,
        List<String> questionImageUrls,
        String answerText,
        LocalDateTime createdAt
) {
    /** 엔티티 → 응답 DTO 변환. */
    public static AiQuestionResponse from(AiQuestion q) {
        // 첨부 이미지들을 sortOrder 순으로 정렬해 FileAsset의 접근 URL 목록으로 펼친다.
        List<String> imageUrls = q.getAttachments().stream()
                .sorted(Comparator.comparing(a -> a.getSortOrder() == null ? 0 : a.getSortOrder()))
                .map(a -> a.getFileAsset().getFileUrl())
                .toList();

        return new AiQuestionResponse(
                q.getId(),
                q.getUser().getId(),
                q.getSubject().getId(),
                q.getQuestionText(),
                imageUrls,
                q.getAnswerText(),
                q.getCreatedAt()
        );
    }
}
