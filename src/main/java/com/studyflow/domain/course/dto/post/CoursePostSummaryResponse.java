package com.studyflow.domain.course.dto.post;

import com.studyflow.domain.course.entity.CoursePost;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 게시글 목록 뷰용 — 댓글 내용 없이 요약 정보만 포함
// 첨부파일은 개수만 내려주고, 전체 메타데이터는 상세 응답(CoursePostDetailResponse)에서만 제공
@Getter
@Builder
public class CoursePostSummaryResponse {

    private Long id;
    private String title;
    private Long authorId;
    private String authorName;
    private int viewCount;
    private long commentCount;
    private int attachmentCount;
    private LocalDateTime createdAt;

    public static CoursePostSummaryResponse of(CoursePost post, long commentCount) {
        return CoursePostSummaryResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .authorId(post.getUser().getId())
                .authorName(post.getUser().getName())
                .viewCount(post.getViewCount())
                .commentCount(commentCount)
                .attachmentCount(post.getAttachments() == null ? 0 : post.getAttachments().size())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
