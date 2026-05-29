package com.studyflow.domain.course.dto.post;

import com.studyflow.domain.course.dto.comment.CoursePostCommentResponse;
import com.studyflow.domain.course.entity.CoursePost;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 게시글 상세 뷰용 — 댓글 목록까지 포함
@Getter
@Builder
public class CoursePostDetailResponse {

    private Long id;
    private Long courseId;
    private String title;
    private String content;
    private int viewCount;
    private Long authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CoursePostCommentResponse> comments;

    public static CoursePostDetailResponse of(CoursePost post, List<CoursePostCommentResponse> comments) {
        return CoursePostDetailResponse.builder()
                .id(post.getId())
                .courseId(post.getCourse().getId())
                .title(post.getTitle())
                .content(post.getContent())
                .viewCount(post.getViewCount())
                .authorId(post.getUser().getId())
                .authorName(post.getUser().getName())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .comments(comments)
                .build();
    }
}
