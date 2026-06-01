package com.studyflow.domain.course.specification;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;

// 수업 검색에 사용되는 동적 필터 조건 모음
// 각 메서드가 null을 반환하면 해당 조건은 무시됨 (필터 미적용)
// Service에서 Specification.where(A).and(B).and(C) 형태로 조합해서 사용
public class CourseSpecification {

    private CourseSpecification() {} // 유틸리티 클래스 — 인스턴스화 방지

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

    // 대상 학년 필터 — 다중 선택, 선택한 학년 중 하나라도 일치하면 노출 (OR 조건)
    public static Specification<Course> hasTargetGrades(List<TargetGrade> targetGrades) {
        return (root, query, cb) -> {
            if (targetGrades == null || targetGrades.isEmpty()) return null;
            return root.get("targetGrade").in(targetGrades);
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

    // 검색 목록 기본 조건: 공개 수업(isListed=true) + 모집 중 또는 수강 중인 수업
    // isListed는 선생님이 직접 토글로 제어하는 노출 여부 플래그
    // IN_PROGRESS 포함: 수강 중인 수업도 선생님이 원하면 검색 노출 가능
    public static Specification<Course> isSearchable() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isListed")),
                root.get("status").in(CourseStatus.RECRUITING, CourseStatus.IN_PROGRESS)
        );
    }
}
