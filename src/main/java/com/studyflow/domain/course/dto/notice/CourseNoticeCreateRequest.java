package com.studyflow.domain.course.dto.notice;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CourseNoticeCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    // 중요 공지로 표시할지 여부 (기본값 false)
    private boolean important = false;
}
