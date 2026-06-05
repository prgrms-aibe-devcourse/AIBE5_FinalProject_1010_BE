package com.studyflow.domain.course.dto.notice;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공지사항 첨부파일 정보 — 엔티티 JSON 컬럼 저장 및 API 응답에 모두 사용
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NoticeAttachmentInfo {
    private String url;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
}
