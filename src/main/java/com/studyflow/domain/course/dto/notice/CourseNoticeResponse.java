package com.studyflow.domain.course.dto.notice;

import com.studyflow.domain.course.entity.CourseNotice;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CourseNoticeResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String content;
    private boolean important;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CourseNoticeResponse from(CourseNotice notice) {
        return CourseNoticeResponse.builder()
                .id(notice.getId())
                .courseId(notice.getCourse().getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .important(notice.isImportant())
                .authorId(notice.getUser().getId())
                .authorName(notice.getUser().getName())
                .createdAt(notice.getCreatedAt())
                .updatedAt(notice.getUpdatedAt())
                .build();
    }
}
