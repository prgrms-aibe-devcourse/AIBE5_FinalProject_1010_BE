package com.studyflow.domain.course.dto.post;

import com.studyflow.domain.course.dto.notice.NoticeAttachmentInfo;
import com.studyflow.domain.course.entity.CoursePost;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

// 게시글 목록 뷰용 — 댓글 내용 없이 요약 정보만 포함
@Getter
@Builder
public class CoursePostSummaryResponse {

    private Long id;
    private String title;
    private Long authorId;
    private String authorName;
    private int viewCount;
    private long commentCount;
    private List<NoticeAttachmentInfo> attachments;
    private LocalDateTime createdAt;

    public static CoursePostSummaryResponse of(CoursePost post, long commentCount) {
        return CoursePostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .authorId(post.getUser().getId())
                .authorName(post.getUser().getName())
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .attachments(post.getAttachments())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
