package com.studyflow.domain.assignment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class AssignmentRequest {

    @NotBlank(message = "과제 제목은 필수입니다.")
    @Size(max = 200, message = "과제 제목은 200자를 초과할 수 없습니다.")
    private String title;

    private String content;

    private LocalDateTime dueDate;
}
