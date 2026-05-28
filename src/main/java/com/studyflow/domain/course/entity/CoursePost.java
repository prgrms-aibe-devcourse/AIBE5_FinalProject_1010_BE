package com.studyflow.domain.course.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    /** 게시글 작성자 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 게시글이 속한 수업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public static CoursePost create(User user, Course course, String title, String content) {
        CoursePost post = new CoursePost();
        post.user = user;
        post.course = course;
        post.title = title;
        post.content = content;
        return post;
    }

    public void update(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
