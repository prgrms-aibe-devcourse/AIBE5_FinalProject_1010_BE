package com.studyflow.domain.teacher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 선생님 찾기 목록 노출 여부 토글 요청
@Getter
@NoArgsConstructor
public class TeacherListedUpdateRequest {

    @NotNull(message = "노출 여부(listed)는 필수입니다.")
    private Boolean listed;
}
