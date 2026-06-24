package com.studyflow.domain.wrongnote.dto.request;

import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;
import jakarta.validation.constraints.NotNull;

public record WrongAnswerNoteReviewRequest(
        @NotNull(message = "복습 결과는 필수입니다.")
        WrongAnswerReviewResult result,

        String memo
) {
}
