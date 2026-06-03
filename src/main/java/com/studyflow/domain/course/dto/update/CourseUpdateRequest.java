package com.studyflow.domain.course.dto.update;

import com.studyflow.domain.course.enums.CurriculumType;
import com.studyflow.domain.course.enums.TargetGrade;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 수업 수정 요청 DTO — 생성 요청과 동일한 필드 구성
// 수정 시 전체 필드를 다시 보내는 방식 (PUT)
@Getter
@NoArgsConstructor
public class CourseUpdateRequest {

    @NotBlank(message = "수업명은 필수입니다.")
    private String title;

    private String description;

    @NotNull(message = "과목은 필수입니다.")
    private Long subjectId;             // 과목 변경 가능

    @NotNull(message = "대상 학년은 필수입니다.")
    private TargetGrade targetGrade;

    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    private Integer maxStudents;        // 선택 — 미입력 시 기존 값 유지

    @Min(value = 1, message = "수업 시간은 1분 이상이어야 합니다.")
    private int durationMinutes;

    @Min(value = 0, message = "수업료는 0원 이상이어야 합니다.")
    private int pricePerSession;

    private String textbook;

    @NotNull(message = "커리큘럼 유형은 필수입니다.")
    private CurriculumType curriculumType;

    private String curriculumDetail;
    private String availableSchedule;
    private String firstClassDate;
    private String thumbnailUrl;
    private LocalDate recruitDeadline;
    private LocalDate startDate;
    private LocalDate endDate;
}
