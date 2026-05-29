package com.studyflow.domain.file.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 파일 업로드 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileUploadResponse {

    private Long fileId;

    private String fileUrl;

    private String thumbnailUrl;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private Integer width;

    private Integer height;
}