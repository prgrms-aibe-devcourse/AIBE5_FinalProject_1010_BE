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
    private String relatedTitle;  // nullable — 참조 대상(답변·수업)이 삭제된 경우 null
    private LocalDateTime createdAt;

    public static NaegongHistoryItem of(NaegongHistory history, String relatedTitle) {
        return NaegongHistoryItem.builder()
                .scoreChange(history.getScoreChange())
                .reason(history.getReason())
                .reasonLabel(history.getReason().getDescription())
                .relatedTitle(relatedTitle)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
