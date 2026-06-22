package com.studyflow.domain.course.dto.search;

import com.studyflow.domain.course.enums.CourseSort;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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

    // 거리순(DISTANCE) 정렬에 사용할 학생의 현재 위치 좌표 (브라우저 GPS)
    // sort=DISTANCE일 때만 의미 있으며, 둘 다 있어야 거리 정렬이 동작한다.
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
    @DecimalMax(value = "90.0",  message = "위도는 90 이하이어야 합니다.")
    private Double studentLat;

    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
    @DecimalMax(value = "180.0",  message = "경도는 180 이하이어야 합니다.")
    private Double studentLng;

    // 거리순(DISTANCE) 정렬에 필요한 좌표가 둘 다 들어왔는지 검증
    // 한쪽만 들어오면 거리 계산이 불가능하므로 차단 (DISTANCE가 아니면 검증하지 않음)
    @AssertTrue(message = "거리순 정렬에는 위도·경도를 함께 전달해야 합니다.")
    public boolean isDistanceSortLocationValid() {
        if (sort != CourseSort.DISTANCE) return true;
        return studentLat != null && studentLng != null;
    }
}
