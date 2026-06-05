package com.studyflow.domain.course.dto.notice;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class CourseNoticeCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 중요 공지로 표시할지 여부 (기본값 false)
    private boolean important = false;

    // 첨부파일 목록 — 프론트에서 파일 업로드 후 받은 메타데이터를 전달
    private List<NoticeAttachmentInfo> attachments = new ArrayList<>();
}
