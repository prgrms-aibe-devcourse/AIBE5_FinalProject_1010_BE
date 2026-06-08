package com.studyflow.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 질문 수정 요청. (PATCH /api/v1/qna/questions/{questionId}, 작성 학생 본인)
 *
 * <p>{@code imageFileIds}는 수정 후 최종 첨부 상태(전체 교체)를 의미한다.
 * null 또는 빈 배열이면 첨부 이미지를 모두 제거한다.</p>
 */
@Getter
@NoArgsConstructor
public class QnaQuestionUpdateRequest {

    @NotNull(message = "과목은 필수입니다.")
    private Long subjectId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    private List<Long> imageFileIds;
}
