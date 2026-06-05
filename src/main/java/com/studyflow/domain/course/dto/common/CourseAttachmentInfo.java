package com.studyflow.domain.course.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 공지사항·게시글 공용 첨부파일 정보 — 엔티티 JSON 컬럼 저장 및 API 응답에 모두 사용
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CourseAttachmentInfo {
    private String url;
    private String originalFileName;
    private Long fileSize;
    private String contentType;
}
