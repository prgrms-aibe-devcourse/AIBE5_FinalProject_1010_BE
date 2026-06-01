package com.studyflow.domain.course.enums;

// 수업 검색 결과 정렬 기준
public enum CourseSort {
    LATEST, // 최신순 (createdAt DESC)
    RATING, // 평점 높은순 (Review AVG DESC) — CourseRepository에 별도 JPQL 쿼리 구현 필요, 미구현 시 LATEST로
            // 폴백됨
    PRICE_ASC, // 가격 낮은순
    PRICE_DESC // 가격 높은순
}
