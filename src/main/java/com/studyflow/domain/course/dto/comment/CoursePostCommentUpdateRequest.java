package com.studyflow.domain.course.dto.comment;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CoursePostCommentUpdateRequest {

    @NotBlank(message = "댓글 내용을 입력해주세요.")
    private String content;
}
