package com.studyflow.domain.course.dto.progress;

import com.studyflow.domain.course.entity.CourseProgress;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class CourseProgressResponse {

    private Long id;
    private Long courseId;
    private LocalDate progressDate;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseProgressResponse from(CourseProgress progress) {
        return CourseProgressResponse.builder()
                .id(progress.getId())
                .courseId(progress.getCourse().getId())
                .progressDate(progress.getProgressDate())
                .content(progress.getContent())
                .authorId(progress.getUser().getId())
                .authorName(progress.getUser().getName())
                .createdAt(progress.getCreatedAt())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}
