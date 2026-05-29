package com.studyflow.domain.course.dto.comment;

import com.studyflow.domain.course.entity.CoursePostComment;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CoursePostCommentResponse {

    private Long id;
    private Long postId;
    private String content;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CoursePostCommentResponse from(CoursePostComment comment) {
        return CoursePostCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getCoursePost().getId())
                .content(comment.getContent())
                .authorId(comment.getUser().getId())
                .authorName(comment.getUser().getName())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
