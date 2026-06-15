package com.studyflow.domain.course.dto.update;

import com.studyflow.domain.course.enums.CurriculumType;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 수업 수정 요청 DTO
// maxStudents만 null 허용(기존 값 유지)이므로 PATCH 의미에 가까움
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

    private TeachingMode teachingMode;
    private String location;
    private Double locationLat;
    private Double locationLng;

    // 날짜 순서 검증: recruitDeadline <= startDate <= endDate
    @AssertTrue(message = "모집 마감일은 수업 시작일보다 이전이어야 합니다.")
    public boolean isRecruitDeadlineBeforeStartDate() {
        if (recruitDeadline == null || startDate == null) return true;
        return !recruitDeadline.isAfter(startDate);
    }

    @AssertTrue(message = "수업 시작일은 종료일보다 이전이어야 합니다.")
    public boolean isStartDateBeforeEndDate() {
        if (startDate == null || endDate == null) return true;
        return !startDate.isAfter(endDate);
    }
}
