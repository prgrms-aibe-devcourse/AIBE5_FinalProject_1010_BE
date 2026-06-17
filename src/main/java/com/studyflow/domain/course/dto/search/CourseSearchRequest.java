package com.studyflow.domain.course.dto.search;

import com.studyflow.domain.course.enums.CourseSort;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CourseSearchRequest {

    // 수업명 또는 설명 키워드 검색
    @Size(max = 100, message = "키워드는 100자 이내로 입력해주세요.")
    private String keyword;

    // 과목 필터 — 다중 선택 가능, 선택한 과목 중 하나라도 일치하면 노출
    @Size(max = 10, message = "과목 필터는 최대 10개까지 선택할 수 있습니다.")
    private List<Long> subjectIds;

    // 학년 필터 — 다중 선택 가능, 선택한 학년 중 하나라도 일치하면 노출
    // TargetGrade 열거형 값이 총 13개로 고정되어 있으므로 max = 13
    @Size(max = 13, message = "학년 필터는 최대 13개까지 선택할 수 있습니다.")
    private List<TargetGrade> targetGrades;

    // 수업 방식 필터 — ONLINE / OFFLINE (null이면 전체)
    private TeachingMode teachingMode;

    // 지역 필터 — 다중 선택 가능, 선택한 지역 중 하나라도 일치하면 노출 (OR 조건)
    // 프론트 지역 선택기가 주는 "경기 고양시 일산동구" 형태의 문자열 목록
    // 대면 수업의 location(전체 주소) 앞부분과 prefix 매칭 (Specification에서 시/도 정식명 보정)
    @Valid
    @Size(max = 20, message = "지역 필터는 최대 20개까지 선택할 수 있습니다.")
    private List<@Size(max = 50, message = "지역 문자열은 50자 이내여야 합니다.") String> regions;

    // 회당 가격 최솟값 필터
    @Min(value = 0, message = "최소 가격은 0원 이상이어야 합니다.")
    private Integer minPrice;

    // 회당 가격 최댓값 필터
    @Min(value = 0, message = "최대 가격은 0원 이상이어야 합니다.")
    private Integer maxPrice;

    // minPrice > maxPrice 교차 검증
    @AssertTrue(message = "최소 가격은 최대 가격보다 클 수 없습니다.")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null)
            return true;
        return minPrice <= maxPrice;
    }

    // 수업 최대 인원 하한 필터 (maxStudents >= minGroupSize)
    @Min(value = 1, message = "최소 인원은 1명 이상이어야 합니다.")
    private Integer minGroupSize;

    // 수업 최대 인원 상한 필터 (maxStudents <= maxGroupSize)
    @Min(value = 1, message = "최대 인원은 1명 이상이어야 합니다.")
    private Integer maxGroupSize;

    // minGroupSize > maxGroupSize 교차 검증
    @AssertTrue(message = "최소 인원은 최대 인원보다 클 수 없습니다.")
    public boolean isGroupSizeRangeValid() {
        if (minGroupSize == null || maxGroupSize == null)
            return true;
        return minGroupSize <= maxGroupSize;
    }

    // 정렬 기준 (기본값: 최신순)
    private CourseSort sort = CourseSort.LATEST;
}
