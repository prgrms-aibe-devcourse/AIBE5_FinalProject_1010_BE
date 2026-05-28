package com.studyflow.domain.course.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 수업 게시판 댓글 엔티티
 *
 * 자유 게시판 게시글에 달리는 댓글.
 * 선생님과 학생 모두 작성 가능.
 */
@Entity
@Table(
        name = "course_post_comment",
        indexes = {
                @Index(name = "idx_course_post_comment_post", columnList = "course_post_id"),
                @Index(name = "idx_course_post_comment_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePostComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글이 달린 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_post_id", nullable = false)
    private CoursePost coursePost;

    // 댓글 작성자
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // null이면 정상 상태, 값이 있으면 소프트 딜리트된 댓글
    @Column
    private LocalDateTime deletedAt;

    public static CoursePostComment create(CoursePost coursePost, User user, String content) {
        CoursePostComment comment = new CoursePostComment();
        comment.coursePost = coursePost;
        comment.user = user;
        comment.content = content;
        return comment;
    }

    public void update(String content) {
        this.content = content;
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
