package com.studyflow.domain.course.entity;

import com.studyflow.domain.course.converter.NoticeAttachmentListConverter;
import com.studyflow.domain.course.dto.notice.NoticeAttachmentInfo;
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
 * 수업 공지사항 엔티티
 *
 * 선생님이 수업별로 작성하는 공지사항.
 * 소프트 딜리트 방식으로 삭제된다 (deletedAt).
 */
@Entity
@Table(
        name = "course_notice",
        indexes = {
                @Index(name = "idx_course_notice_course", columnList = "course_id"),
                @Index(name = "idx_course_notice_user", columnList = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseNotice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 공지를 작성한 선생님
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 공지가 속한 수업
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // 중요 공지 여부 (상단 고정 등에 활용)
    @Column(name = "is_important", nullable = false)
    private boolean important = false;

    // 첨부파일 목록 — JSON으로 직렬화해 TEXT 컬럼에 저장
    @Convert(converter = NoticeAttachmentListConverter.class)
    @Column(name = "attachments", columnDefinition = "TEXT")
    private List<NoticeAttachmentInfo> attachments = new ArrayList<>();

    // null이면 정상 상태, 값이 있으면 소프트 딜리트된 공지
    @Column
    private LocalDateTime deletedAt;

    public static CourseNotice create(
            User user, Course course,
            String title, String content, boolean important,
            List<NoticeAttachmentInfo> attachments) {
        CourseNotice notice = new CourseNotice();
        notice.user = user;
        notice.course = course;
        notice.title = title;
        notice.content = content;
        notice.important = important;
        notice.attachments = attachments != null ? attachments : new ArrayList<>();
        return notice;
    }

    public void update(String title, String content, boolean important,
                       List<NoticeAttachmentInfo> attachments) {
        this.title = title;
        this.content = content;
        this.important = important;
        this.attachments = attachments != null ? attachments : new ArrayList<>();
    }

    public void delete() {
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}
