package com.studyflow.domain.course.dto.create;

import com.studyflow.domain.course.enums.CurriculumType;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

// 수업 등록 요청 DTO — 선생님이 마이페이지에서 수업을 생성할 때 사용
// *표시된 필드는 필수, 나머지는 선택
@Getter
@NoArgsConstructor
public class CourseCreateRequest {

    @NotBlank(message = "수업명은 필수입니다.")
    private String title;               // * 수업명 (예: "고2 미적분 1:1 과외")

    private String description;         // 수업 소개글

    @NotNull(message = "과목은 필수입니다.")
    private Long subjectId;             // * 과목 ID (subject 테이블 참조)

    @NotNull(message = "대상 학년은 필수입니다.")
    private TargetGrade targetGrade;    // * 대상 학년 (예: HIGH_2)

    @Min(value = 1, message = "정원은 1명 이상이어야 합니다.")
    private Integer maxStudents;        // 최대 수강 인원 (선택 — 미입력 시 기본값 1)

    @Min(value = 1, message = "수업 시간은 1분 이상이어야 합니다.")
    private int durationMinutes;        // * 회당 수업 시간(분) (예: 60, 90)

    @Min(value = 0, message = "수업료는 0원 이상이어야 합니다.")
    private int pricePerSession;        // * 회당 수업료(원)

    private String textbook;            // 사용 교재 (예: "수능 기출 + 선생님 자료")

    @NotNull(message = "커리큘럼 유형은 필수입니다.")
    private CurriculumType curriculumType;  // * CUSTOM(학생 수준 맞춤) / FIXED(정해진 커리큘럼)

    private String curriculumDetail;    // 커리큘럼 상세 내용
    private String availableSchedule;   // 가능한 요일·시간대 (예: "평일 저녁 7~10시")
    private String firstClassDate;      // 첫 수업 가능 시기 (예: "즉시", "다음 주부터")
    private String thumbnailUrl;        // 썸네일 이미지 URL
    private LocalDate recruitDeadline;  // 모집 마감일
    private LocalDate startDate;        // 수업 시작일
    private LocalDate endDate;          // 수업 종료일

    private TeachingMode teachingMode;  // 수업 방식: ONLINE(비대면) / OFFLINE(대면)

    @Size(max = 300, message = "수업 장소는 300자 이내여야 합니다.")
    private String location;            // 대면 수업 장소 주소 (OFFLINE일 때만 의미 있음)

    @DecimalMin(value = "-90.0",  message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0",   message = "위도는 90 이하이어야 합니다.")
    private Double locationLat;         // 위도

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0",  message = "경도는 180 이하이어야 합니다.")
    private Double locationLng;         // 경도

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

    @AssertTrue(message = "대면 수업은 장소 주소를 입력해야 합니다.")
    public boolean isLocationRequiredForOffline() {
        return teachingMode != TeachingMode.OFFLINE
                || (location != null && !location.isBlank());
    }
}
