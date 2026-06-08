package com.studyflow.domain.naegong.enums;

/**
 * 내공 점수 변동 사유.
 *
 * <p>현재는 QnA 답변 채택만 존재하지만, 이후 리뷰/수업 완료 등으로 확장될 수 있다.</p>
 */
public enum NaegongReason {
    ANSWER_ACCEPTED("QnA 답변 채택");

    private final String description;

    NaegongReason(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
