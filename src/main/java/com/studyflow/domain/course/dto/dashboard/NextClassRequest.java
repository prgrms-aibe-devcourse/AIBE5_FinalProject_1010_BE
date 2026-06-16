package com.studyflow.domain.course.dto.dashboard;

import jakarta.validation.constraints.FutureOrPresent;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class NextClassRequest {
    // null 허용 — null 전송 시 다음 수업 일정 삭제
    @FutureOrPresent(message = "다음 수업 일시는 현재 이후여야 합니다.")
    private LocalDateTime nextClassAt;
}
