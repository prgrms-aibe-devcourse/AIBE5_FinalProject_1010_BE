package com.studyflow.domain.course.entity;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수업 공지사항 첨부파일 엔티티
 *
 * 공지 1개에 파일 여러 개를 순서대로 연결하는 매핑 테이블.
 */
@Entity
@Table(
        name = "course_notice_attachment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_notice_attachment_notice_file",
                        columnNames = {"course_notice_id", "file_asset_id"}
                )
        },
        indexes = {
                @Index(name = "idx_course_notice_attachment_notice", columnList = "course_notice_id"),
                @Index(name = "idx_course_notice_attachment_file", columnList = "file_asset_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CourseNoticeAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 첨부파일이 연결된 공지사항
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_notice_id", nullable = false)
    private CourseNotice courseNotice;

    // 실제 파일 메타데이터
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_asset_id", nullable = false)
    private FileAsset fileAsset;

    // 공지에 파일 여러 개일 때 표시 순서
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public static CourseNoticeAttachment create(CourseNotice courseNotice, FileAsset fileAsset, int sortOrder) {
        CourseNoticeAttachment attachment = new CourseNoticeAttachment();
        attachment.courseNotice = courseNotice;
        attachment.fileAsset = fileAsset;
        attachment.sortOrder = sortOrder;
        return attachment;
    }
}
