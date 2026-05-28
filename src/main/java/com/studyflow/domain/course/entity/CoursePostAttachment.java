package com.studyflow.domain.course.entity;

import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수업 게시판 게시글 첨부파일 엔티티
 *
 * 게시글 1개에 파일 여러 개를 순서대로 연결하는 매핑 테이블.
 */
@Entity
@Table(
        name = "course_post_attachment",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_post_attachment_post_file",
                        columnNames = {"course_post_id", "file_asset_id"}
                )
        },
        indexes = {
                @Index(name = "idx_course_post_attachment_post", columnList = "course_post_id"),
                @Index(name = "idx_course_post_attachment_file", columnList = "file_asset_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CoursePostAttachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 첨부파일이 연결된 게시글
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_post_id", nullable = false)
    private CoursePost coursePost;

    // 실제 파일 메타데이터
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_asset_id", nullable = false)
    private FileAsset fileAsset;

    // 게시글에 파일 여러 개일 때 표시 순서
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;

    public static CoursePostAttachment create(CoursePost coursePost, FileAsset fileAsset, int sortOrder) {
        CoursePostAttachment attachment = new CoursePostAttachment();
        attachment.coursePost = coursePost;
        attachment.fileAsset = fileAsset;
        attachment.sortOrder = sortOrder;
        return attachment;
    }
}
