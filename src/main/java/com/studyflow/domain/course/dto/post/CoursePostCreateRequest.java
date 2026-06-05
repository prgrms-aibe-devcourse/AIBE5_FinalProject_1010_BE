package com.studyflow.domain.course.dto.post;

import com.studyflow.domain.course.dto.notice.NoticeAttachmentInfo;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class CoursePostCreateRequest {

    @NotBlank(message = "제목을 입력해주세요.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    private String content;

    private List<NoticeAttachmentInfo> attachments = new ArrayList<>();
}
