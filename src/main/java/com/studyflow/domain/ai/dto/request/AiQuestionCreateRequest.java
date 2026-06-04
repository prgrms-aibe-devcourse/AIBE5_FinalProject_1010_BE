package com.studyflow.domain.ai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * AI 질문 요청 바디. (POST /api/v1/ai/questions)
 *
 * <p>명세(§26)는 이미지를 {@code questionImageUrl}(단수 URL)로 받지만, 본 구현은
 * 프로젝트의 파일 처리 컨벤션(채팅 첨부)에 맞춰 <b>업로드 후 받은 fileId 목록</b>을 받는다.
 * 한 질문에 이미지를 여러 장 붙일 수 있다(여러 페이지 문제, 문제+풀이 등).
 * 클라이언트는 먼저 파일 업로드 API로 이미지들을 올려 각 fileId를 받은 뒤 그 목록을 전달한다.
 * (명세와 달라지는 지점)</p>
 * <pre>
 * {
 *   "subjectId": 2,
 *   "questionText": "이 문제들 풀이 과정을 알려주세요.",
 *   "questionImageFileIds": [12, 13]
 * }
 * </pre>
 *
 * @param subjectId            질문 과목 id (필수)
 * @param questionText         질문 본문 (필수)
 * @param questionImageFileIds 첨부 이미지들의 FileAsset id 목록 (선택, 없으면 null 또는 빈 배열)
 * @param conversationId       이어서 질문할 대화 id (선택). null이면 새 대화를 시작한다.
 */
public record AiQuestionCreateRequest(

        @NotNull(message = "과목을 선택해주세요.")
        Long subjectId,

        @NotBlank(message = "질문 내용을 입력해주세요.")
        @Size(max = 2000, message = "질문은 2000자 이내로 입력해주세요.")
        String questionText,

        List<Long> questionImageFileIds,

        Long conversationId
) {
}
