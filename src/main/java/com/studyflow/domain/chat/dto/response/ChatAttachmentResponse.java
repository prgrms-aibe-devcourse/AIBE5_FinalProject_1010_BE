package com.studyflow.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 채팅 메시지 첨부파일 응답.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatAttachmentResponse {

    private Long fileId;

    private String fileUrl;

    private String thumbnailUrl;

    private String originalFileName;

    private String contentType;

    private Long fileSize;

    private Integer width;

    private Integer height;

    private Integer sortOrder;
}