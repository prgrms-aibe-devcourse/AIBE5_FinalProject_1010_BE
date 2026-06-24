package com.studyflow.domain.file.entity;

import com.studyflow.domain.file.enums.FileCategory;
import com.studyflow.domain.file.enums.FileStorageProvider;
import com.studyflow.domain.file.enums.FileUploadStatus;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(
        name = "file_asset",
        indexes = {
                @Index(name = "idx_file_asset_uploader", columnList = "uploader_id"),
                @Index(name = "idx_file_asset_category", columnList = "file_category"),
                @Index(name = "idx_file_asset_upload_status", columnList = "upload_status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FileAsset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 파일 업로더.
     *
     * 채팅 이미지, 게시판 첨부 등 사용자가 올리는 파일이므로 NOT NULL.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    /**
     * 사용자가 올린 원본 파일명.
     */
    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    /**
     * 서버/S3에 저장된 파일명.
     *
     * UUID 기반 추천.
     */
    @Column(name = "stored_file_name", nullable = false, length = 255)
    private String storedFileName;

    /**
     * 저장소 종류.
     *
     * LOCAL / S3
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "storage_provider", nullable = false, length = 20)
    private FileStorageProvider storageProvider;

    /**
     * S3 bucket 이름.
     */
    @Column(name = "bucket", length = 100)
    private String bucket;

    /**
     * S3 object key.
     *
     * 예: chat/2026/05/uuid.png
     */
    @Column(name = "object_key", length = 500)
    private String objectKey;

    /**
     * 파일 접근 URL.
     */
    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    /**
     * 썸네일 URL.
     */
    @Column(name = "thumbnail_url", length = 500)
    private String thumbnailUrl;

    /**
     * MIME 타입.
     *
     * 예: image/png, image/jpeg
     */
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;

    /**
     * 파일 크기.
     *
     * byte 단위.
     */
    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    /**
     * 파일 분류.
     *
     * IMAGE / FILE / PDF / VIDEO / AUDIO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", nullable = false, length = 20)
    private FileCategory fileCategory;

    /**
     * 이미지 가로 크기.
     */
    @Column(name = "width")
    private Integer width;

    /**
     * 이미지 세로 크기.
     */
    @Column(name = "height")
    private Integer height;

    /**
     * 업로드 상태.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private FileUploadStatus uploadStatus;

    /**
     * 파일 삭제 여부.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 파일 삭제 시각.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public static FileAsset createImage(
            User uploader,
            String originalFileName,
            String storedFileName,
            FileStorageProvider storageProvider,
            String bucket,
            String objectKey,
            String fileUrl,
            String thumbnailUrl,
            String contentType,
            Long fileSize,
            Integer width,
            Integer height
    ) {
        if (uploader == null) {
            throw new IllegalArgumentException("파일 업로더는 필수입니다.");
        }

        FileAsset fileAsset = new FileAsset();
        fileAsset.uploader = uploader;
        fileAsset.originalFileName = originalFileName;
        fileAsset.storedFileName = storedFileName;
        fileAsset.storageProvider = storageProvider;
        fileAsset.bucket = bucket;
        fileAsset.objectKey = objectKey;
        fileAsset.fileUrl = fileUrl;
        fileAsset.thumbnailUrl = thumbnailUrl;
        fileAsset.contentType = contentType;
        fileAsset.fileSize = fileSize;
        fileAsset.fileCategory = FileCategory.IMAGE;
        fileAsset.width = width;
        fileAsset.height = height;
        fileAsset.uploadStatus = FileUploadStatus.COMPLETED;
        return fileAsset;
    }

    // 이미지 외 파일(PDF 등)을 저장할 때 사용하는 범용 팩토리 메서드
    public static FileAsset createFile(
            User uploader,
            String originalFileName,
            String storedFileName,
            FileStorageProvider storageProvider,
            String bucket,
            String objectKey,
            String fileUrl,
            String contentType,
            Long fileSize,
            FileCategory fileCategory
    ) {
        if (uploader == null) {
            throw new IllegalArgumentException("파일 업로더는 필수입니다.");
        }
        FileAsset fileAsset = new FileAsset();
        fileAsset.uploader = uploader;
        fileAsset.originalFileName = originalFileName;
        fileAsset.storedFileName = storedFileName;
        fileAsset.storageProvider = storageProvider;
        fileAsset.bucket = bucket;
        fileAsset.objectKey = objectKey;
        fileAsset.fileUrl = fileUrl;
        fileAsset.contentType = contentType;
        fileAsset.fileSize = fileSize;
        fileAsset.fileCategory = fileCategory;
        fileAsset.uploadStatus = FileUploadStatus.COMPLETED;
        return fileAsset;
    }

    public void markUploading() {
        this.uploadStatus = FileUploadStatus.UPLOADING;
    }

    public void completeUpload(String fileUrl, String thumbnailUrl) {
        this.fileUrl = fileUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.uploadStatus = FileUploadStatus.COMPLETED;
    }

    public void failUpload() {
        this.uploadStatus = FileUploadStatus.FAILED;
    }

    public void delete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
    }

    public boolean isImage() {
        return this.fileCategory == FileCategory.IMAGE;
    }

    public boolean isUsable() {
        return !this.deleted && this.uploadStatus == FileUploadStatus.COMPLETED;
    }
}