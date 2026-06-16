package com.studyflow.domain.assignment.dto;

import com.studyflow.domain.assignment.entity.Assignment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AssignmentResponse {

    private Long id;
    private String title;
    private String content;
    private LocalDateTime dueDate;
    private LocalDateTime createdAt;

    public static AssignmentResponse of(Assignment a) {
        return AssignmentResponse.builder()
                .id(a.getId())
                .title(a.getTitle())
                .content(a.getContent())
                .dueDate(a.getDueDate())
                .createdAt(a.getCreatedAt())
                .build();
    }
}
