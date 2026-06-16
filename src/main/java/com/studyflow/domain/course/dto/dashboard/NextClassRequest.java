package com.studyflow.domain.course.dto.dashboard;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NextClassRequest {
    private LocalDateTime nextClassAt;
}
