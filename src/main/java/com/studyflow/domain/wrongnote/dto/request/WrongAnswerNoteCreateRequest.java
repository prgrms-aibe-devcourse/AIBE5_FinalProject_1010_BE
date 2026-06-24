package com.studyflow.domain.wrongnote.dto.request;

import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;
import jakarta.validation.constraints.Size;

import java.util.List;

public record WrongAnswerNoteCreateRequest(
        Long subjectId,

        @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
        String title,

        String questionContent,
        String answerContent,
        String explanation,
        String wrongReason,
        String memo,

        List<@Size(max = 50, message = "태그는 50자 이하여야 합니다.") String> tags,

        WrongAnswerSourceType sourceType,
        Long sourceQuestionId,
        Long sourceAnswerId
) {
}
