package com.studyflow.domain.course.enums;

// 수업 검색 결과 정렬 기준
public enum CourseSort {
    LATEST,     // 최신순 (createdAt DESC)
    OLDEST,     // 오래된순 (createdAt ASC)
    PRICE_ASC,  // 가격 낮은순
    PRICE_DESC, // 가격 높은순
    DISTANCE    // 가까운순 (학생 위치 기준, 대면 수업만) — studentLat/studentLng 필요
}
