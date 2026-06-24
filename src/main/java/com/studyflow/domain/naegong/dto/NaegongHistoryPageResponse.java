package com.studyflow.domain.naegong.dto;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

@Getter
@Builder
public class NaegongHistoryPageResponse {

    private int totalScore;
    private Page<NaegongHistoryItem> histories;

    public static NaegongHistoryPageResponse of(int totalScore, Page<NaegongHistoryItem> histories) {
        return NaegongHistoryPageResponse.builder()
                .totalScore(totalScore)
                .histories(histories)
                .build();
    }
}
