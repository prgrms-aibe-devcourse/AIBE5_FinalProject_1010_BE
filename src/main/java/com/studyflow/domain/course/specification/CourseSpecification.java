package com.studyflow.domain.course.specification;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // 과목 필터 — 다중 선택, 선택한 과목 중 하나라도 일치하면 노출 (OR 조건)
    public static Specification<Course> hasSubjects(List<Long> subjectIds) {
        return (root, query, cb) -> {
            if (subjectIds == null || subjectIds.isEmpty()) return null;
            return root.get("subject").get("id").in(subjectIds);
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

    // 최소 인원 필터 (maxStudents >= minGroupSize)
    public static Specification<Course> hasMinGroupSize(Integer minGroupSize) {
        return (root, query, cb) -> {
            if (minGroupSize == null) return null;
            return cb.greaterThanOrEqualTo(root.get("maxStudents"), minGroupSize);
        };
    }

    // 최대 인원 필터 (maxStudents <= maxGroupSize)
    public static Specification<Course> hasMaxGroupSize(Integer maxGroupSize) {
        return (root, query, cb) -> {
            if (maxGroupSize == null) return null;
            return cb.lessThanOrEqualTo(root.get("maxStudents"), maxGroupSize);
        };
    }

    // 수업 방식 필터 (ONLINE / OFFLINE)
    public static Specification<Course> hasTeachingMode(TeachingMode teachingMode) {
        return (root, query, cb) -> {
            if (teachingMode == null) return null;
            return cb.equal(root.get("teachingMode"), teachingMode);
        };
    }

    // 시/도 약식명(지역 선택기 출력) → location(카카오 주소)에 저장되는 정식명 보정 테이블.
    // location은 시작점에 고정(anchored)해 매칭하므로 시/도명을 정식명으로 완전히 치환해야
    // "경기 고양시" → "경기도 고양시"처럼 공백 위치가 어긋나지 않는다.
    // (강원/전북은 특별자치도 개편 이후 명칭 기준 — 카카오 주소가 현재 반환하는 형태)
    private static final Map<String, String> SIDO_FULL_NAME = Map.ofEntries(
            Map.entry("서울", "서울특별시"),
            Map.entry("부산", "부산광역시"),
            Map.entry("대구", "대구광역시"),
            Map.entry("인천", "인천광역시"),
            Map.entry("광주", "광주광역시"),
            Map.entry("대전", "대전광역시"),
            Map.entry("울산", "울산광역시"),
            Map.entry("세종", "세종특별자치시"),
            Map.entry("경기", "경기도"),
            Map.entry("강원", "강원특별자치도"),
            Map.entry("충북", "충청북도"),
            Map.entry("충남", "충청남도"),
            Map.entry("전북", "전북특별자치도"),
            Map.entry("전남", "전라남도"),
            Map.entry("경북", "경상북도"),
            Map.entry("경남", "경상남도"),
            Map.entry("제주", "제주특별자치도")
    );

    // 지역 필터 — 다중 선택, 선택한 지역 중 하나라도 location 앞부분과 일치하면 노출 (OR 조건)
    // 예) 선택 "경기 고양시 일산동구" → location "경기도 고양시 일산동구 정발산로 115" 매칭
    //   - 시/도 약식("경기")을 정식("경기도")으로 보정해 location 시작점에 고정(anchored)
    //   - 시/군/구 토큰은 그대로 이어 붙여 그 아래 상세주소만 와일드카드(%)로 허용
    //   - 온라인 수업은 location이 null이라 자연스럽게 제외됨
    public static Specification<Course> hasRegions(List<String> regions) {
        return (root, query, cb) -> {
            if (regions == null || regions.isEmpty()) return null;
            List<Predicate> ors = new ArrayList<>();
            for (String region : regions) {
                if (region == null || region.isBlank()) continue;
                ors.add(cb.like(root.get("location"), toLocationPrefixPattern(region)));
            }
            if (ors.isEmpty()) return null;
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    // "경기 고양시 일산동구" → "경기도 고양시 일산동구%" 형태의 LIKE 패턴 생성
    private static String toLocationPrefixPattern(String region) {
        String[] tokens = region.trim().split("\\s+");
        String rawSido = tokens[0];
        String sido = SIDO_FULL_NAME.getOrDefault(rawSido, rawSido);
        StringBuilder sb = new StringBuilder(sido);
        for (int i = 1; i < tokens.length; i++) {
            // "세종 세종시"의 "세종시"는 시/도 자체를 가리키는 토큰이라
            // 카카오 주소("세종특별자치시 …")에 존재하지 않으므로 패턴에서 제외.
            // (제주시·서귀포시는 제주도 하위 행정시이므로 제외 대상 아님)
            if (rawSido.equals("세종") && tokens[i].equals("세종시")) continue;
            sb.append(' ').append(tokens[i]);
        }
        return sb.append('%').toString();
    }

    // 검색 목록 기본 조건: 공개 수업(isListed=true) + 모집 중인 수업만
    // IN_PROGRESS(수업 진행 중)는 신규 수강생을 받지 않으므로 검색 노출에서 제외
    public static Specification<Course> isSearchable() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isListed")),
                cb.equal(root.get("status"), CourseStatus.RECRUITING)
        );
    }
}
