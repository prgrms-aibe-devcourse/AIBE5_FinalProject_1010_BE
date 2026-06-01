package com.studyflow.domain.course.dto.search;

import com.studyflow.domain.course.enums.CourseSort;
import com.studyflow.domain.course.enums.TargetGrade;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CourseSearchRequest {

    // 수업명 또는 설명 키워드 검색
    private String keyword;

    // 과목 필터 (subject.id)
    private Long subjectId;

    // 학년 필터 (예: HIGH_1, MIDDLE_3)
    private TargetGrade targetGrade;

    // 회당 가격 최솟값 필터
    private Integer minPrice;

    // 회당 가격 최댓값 필터
    private Integer maxPrice;

    // 정렬 기준 (기본값: 최신순)
    private CourseSort sort = CourseSort.LATEST;
}
