package com.studyflow.domain.course.dto.notice;

import com.studyflow.domain.course.dto.common.CourseAttachmentInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @Size(max = 10, message = "첨부파일은 최대 10개까지 등록할 수 있습니다.")
    private List<CourseAttachmentInfo> attachments = new ArrayList<>();
}
