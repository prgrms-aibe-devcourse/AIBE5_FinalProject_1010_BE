package com.studyflow.domain.course.dto.progress;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
public class CourseProgressCreateRequest {

    @NotBlank(message = "진도 내용을 입력해주세요.")
    @Size(max = 1000, message = "진도 내용은 최대 1000자까지 입력할 수 있습니다.")
    private String content;

    // 진도 날짜 — 비우면 서버에서 오늘 날짜로 저장(강의실 나가기 전 그날 진도 기록 등)
    private LocalDate progressDate;
}
