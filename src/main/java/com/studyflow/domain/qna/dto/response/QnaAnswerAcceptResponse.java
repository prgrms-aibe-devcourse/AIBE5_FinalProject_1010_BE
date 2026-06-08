package com.studyflow.domain.qna.dto.response;

/** 답변 채택 결과. 채택된 답변과 그로 인해 적립된 선생님 내공 점수를 함께 반환한다. */
public record QnaAnswerAcceptResponse(
        Long answerId,
        Long questionId,
        Long teacherUserId,
        boolean isAccepted,
        boolean questionResolved,
        int addedNaegongScore,
        int teacherNaegongScore
) {
}
