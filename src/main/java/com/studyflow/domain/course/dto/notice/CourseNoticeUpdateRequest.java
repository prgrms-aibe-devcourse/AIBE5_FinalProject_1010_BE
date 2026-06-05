package com.studyflow.domain.course.dto.notice;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class CourseNoticeUpdateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private boolean important = false;

    // 수정 후 남길 첨부파일 목록 (제거된 파일은 포함하지 않음)
    private List<NoticeAttachmentInfo> attachments = new ArrayList<>();
}
