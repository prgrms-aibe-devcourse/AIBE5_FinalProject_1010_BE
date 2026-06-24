package com.studyflow.domain.naegong.dto;

import com.studyflow.domain.naegong.entity.NaegongHistory;
import com.studyflow.domain.naegong.enums.NaegongReason;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NaegongHistoryItem {

    private int scoreChange;
    private NaegongReason reason;
    private String reasonLabel;
    // ANSWER_ACCEPTED: answerId / CLASSROOM_SESSION_CLOSED: courseId / nullable(이력 정리 시 null 가능)
    private Long referenceId;
    // ANSWER_ACCEPTED일 때만 QnaAnswer.question.id. 그 외 또는 참조 삭제 시 null.
    private Long questionId;
    private String relatedTitle;  // nullable — 참조 대상(답변·수업)이 삭제된 경우 null
    private LocalDateTime createdAt;

    public static NaegongHistoryItem of(NaegongHistory history, String relatedTitle, Long questionId) {
        return NaegongHistoryItem.builder()
                .scoreChange(history.getScoreChange())
                .reason(history.getReason())
                .reasonLabel(history.getReason().getDescription())
                .referenceId(history.getReferenceId())
                .questionId(questionId)
                .relatedTitle(relatedTitle)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
