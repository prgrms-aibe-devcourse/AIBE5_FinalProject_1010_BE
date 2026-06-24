package com.studyflow.domain.course.entity;

import com.studyflow.domain.course.converter.NoticeAttachmentListConverter;
import com.studyflow.domain.course.dto.common.CourseAttachmentInfo;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 수업 자유 게시판 게시글 엔티티
 *
 * 선생님과 학생 모두 작성 가능한 수업별 자유 게시판.
 */
@Entity
@Table(
        name = "course_post",
        indexes = {
                @Index(name = "idx_course_post_course", columnList = "course_id"),
                @Index(name = "idx_course_post_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 게시글 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 게시글이 속한 수업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private int viewCount = 0;

    // 첨부파일 목록 — JSON으로 직렬화해 TEXT 컬럼에 저장
    @Convert(converter = NoticeAttachmentListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    private List<CourseAttachmentInfo> attachments = new ArrayList<>();

    // null이면 정상 상태, 값이 있으면 소프트 딜리트된 게시글
    @Column
    private LocalDateTime deletedAt;

    public static CoursePost create(User user, Course course, String title, String content,
                                    List<CourseAttachmentInfo> attachments) {
        CoursePost post = new CoursePost();
        post.user = user;
        post.course = course;
        post.title = title;
        post.content = content;
        post.attachments = attachments != null ? attachments : new ArrayList<>();
        return post;
    }

    public void update(String title, String content, List<CourseAttachmentInfo> attachments) {
        this.title = title;
        this.content = content;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public void incrementViewCount() {
        this.viewCount++;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
