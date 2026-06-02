package com.studyflow.domain.course.enums;

// 수업 검색 결과 정렬 기준
public enum CourseSort {
    LATEST,     // 최신순 (createdAt DESC)
    PRICE_ASC,  // 가격 낮은순
    PRICE_DESC  // 가격 높은순
}
