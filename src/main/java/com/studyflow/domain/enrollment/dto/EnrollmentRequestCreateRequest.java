package com.studyflow.domain.enrollment.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 수강 신청 요청 — 신청하기 모달에서 학생이 작성하는 내용 (introduction만 필수)
@Getter
@NoArgsConstructor
public class EnrollmentRequestCreateRequest {

    @NotBlank(message = "자기소개는 필수입니다.")
    private String introduction;        // 자기소개 (학년, 현재 수준 등)

    private String goal;                // 학습 목표 (예: 수능 1등급)
    private String preferredScheduleNote;  // 희망 수업 요일·시간대
    private String preferredStart;      // 첫 수업 희망 시기 (예: 즉시, 다음 주부터)
    private String message;             // 선생님께 한마디
}
