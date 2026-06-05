package com.studyflow.domain.course.dto.post;

import com.studyflow.domain.course.dto.common.CourseAttachmentInfo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    @Size(max = 10, message = "첨부파일은 최대 10개까지 등록할 수 있습니다.")
    private List<CourseAttachmentInfo> attachments = new ArrayList<>();
}
