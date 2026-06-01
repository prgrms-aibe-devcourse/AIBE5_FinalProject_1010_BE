package com.studyflow.domain.course.specification;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import org.springframework.data.jpa.domain.Specification;

// 수업 검색에 사용되는 동적 필터 조건 모음
// 각 메서드가 null을 반환하면 해당 조건은 무시됨 (필터 미적용)
// Service에서 Specification.where(A).and(B).and(C) 형태로 조합해서 사용
public class CourseSpecification {

    // 수업명 또는 설명에 키워드 포함 여부 (부분 일치)
    public static Specification<Course> hasKeyword(String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) return null;
            String pattern = "%" + keyword + "%";
            return cb.or(
                    cb.like(root.get("title"), pattern),
                    cb.like(root.get("description"), pattern)
            );
        };
    }

    // 특정 과목 필터
    public static Specification<Course> hasSubject(Long subjectId) {
        return (root, query, cb) -> {
            if (subjectId == null) return null;
            return cb.equal(root.get("subject").get("id"), subjectId);
        };
    }

    // 대상 학년 필터
    public static Specification<Course> hasTargetGrade(TargetGrade targetGrade) {
        return (root, query, cb) -> {
            if (targetGrade == null) return null;
            return cb.equal(root.get("targetGrade"), targetGrade);
        };
    }

    // 최소 가격 필터 (pricePerSession >= minPrice)
    public static Specification<Course> hasMinPrice(Integer minPrice) {
        return (root, query, cb) -> {
            if (minPrice == null) return null;
            return cb.greaterThanOrEqualTo(root.get("pricePerSession"), minPrice);
        };
    }

    // 최대 가격 필터 (pricePerSession <= maxPrice)
    public static Specification<Course> hasMaxPrice(Integer maxPrice) {
        return (root, query, cb) -> {
            if (maxPrice == null) return null;
            return cb.lessThanOrEqualTo(root.get("pricePerSession"), maxPrice);
        };
    }

    // 검색 목록 기본 조건: 공개 수업(isListed=true) + 모집 중(status=RECRUITING)
    // 모든 검색 요청에 항상 적용
    public static Specification<Course> isSearchable() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isListed")),
                cb.equal(root.get("status"), CourseStatus.RECRUITING)
        );
    }
}
