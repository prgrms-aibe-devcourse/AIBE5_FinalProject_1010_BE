package com.studyflow.domain.course.specification;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import jakarta.persistence.criteria.Expression;
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

    // 시/도 약식명(지역 선택기 출력) → location(카카오 주소)에 저장되는 현행 정식명 보정 테이블.
    // location은 시작점에 고정(anchored)해 매칭하므로 시/도명을 정식명으로 완전히 치환해야
    // "경기 고양시" → "경기도 고양시"처럼 공백 위치가 어긋나지 않는다.
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

    // 강원/전북은 2023년 특별자치도 개편 전에 카카오가 "강원도"/"전라북도"로 반환했으므로,
    // 그 시기에 저장된 location과도 매칭될 수 있도록 구 명칭을 추가로 허용한다.
    private static final Map<String, String> SIDO_LEGACY_NAME = Map.of(
            "강원", "강원도",
            "전북", "전라북도"
    );

    // LIKE 패턴 내 와일드카드 문자를 이스케이프하는 문자 (백슬래시)
    private static final char ESCAPE_CHAR = '\\';

    // 지역 필터 — 다중 선택, 선택한 지역 중 하나라도 location 앞부분과 일치하면 노출 (OR 조건)
    // 예) 선택 "경기 고양시 일산동구" → location "경기도 고양시 일산동구 정발산로 115" 매칭
    //   - 시/도 약식("경기")을 정식("경기도")으로 보정해 location 시작점에 고정(anchored)
    //   - 강원/전북은 구 명칭(강원도/전라북도)도 OR로 함께 매칭 (개편 이전 저장 데이터 방어)
    //   - region 토큰의 %, _ 는 이스케이프해 와일드카드로 오해석되지 않도록 차단
    //   - 온라인 수업은 location이 null이라 자연스럽게 제외됨
    public static Specification<Course> hasRegions(List<String> regions) {
        return (root, query, cb) -> {
            if (regions == null || regions.isEmpty()) return null;
            List<Predicate> ors = new ArrayList<>();
            for (String region : regions) {
                if (region == null || region.isBlank()) continue;
                String rawSido = region.trim().split("\\s+")[0];
                // 현행 정식명으로 매칭
                ors.add(cb.like(root.get("location"),
                        toLocationPrefixPattern(region, SIDO_FULL_NAME.getOrDefault(rawSido, rawSido)),
                        ESCAPE_CHAR));
                // 강원/전북: 개편 이전 구 명칭으로도 추가 매칭
                if (SIDO_LEGACY_NAME.containsKey(rawSido)) {
                    ors.add(cb.like(root.get("location"),
                            toLocationPrefixPattern(region, SIDO_LEGACY_NAME.get(rawSido)),
                            ESCAPE_CHAR));
                }
            }
            if (ors.isEmpty()) return null;
            return cb.or(ors.toArray(new Predicate[0]));
        };
    }

    // "경기 고양시 일산동구" + sido("경기도") → "경기도 고양시 일산동구%" 형태의 LIKE 패턴 생성.
    // 시/군/구 토큰의 %, _ 는 이스케이프해 와일드카드로 오해석되지 않도록 차단한다.
    private static String toLocationPrefixPattern(String region, String sido) {
        String[] tokens = region.trim().split("\\s+");
        String rawSido = tokens[0];
        StringBuilder sb = new StringBuilder(sido);
        for (int i = 1; i < tokens.length; i++) {
            // "세종 세종시"의 "세종시"는 카카오 주소에 존재하지 않으므로 제외
            // (제주시·서귀포시는 제주도 하위 행정시이므로 유지)
            if (rawSido.equals("세종") && tokens[i].equals("세종시")) continue;
            sb.append(' ').append(escapeLike(tokens[i]));
        }
        return sb.append('%').toString();
    }

    // LIKE 패턴 내 와일드카드 문자(%, _) 이스케이프.
    // 처리 순서가 중요: 백슬래시(이스케이프 문자 자체)를 반드시 먼저 치환해야 한다.
    // 순서가 바뀌면 % → \% 변환 후 다시 \ → \\ 로 치환되어 \\% 가 되는 이중 이스케이프 버그 발생.
    private static String escapeLike(String token) {
        return token.replace("\\", "\\\\")   // 1) 이스케이프 문자 자체를 먼저 처리
                    .replace("%", ESCAPE_CHAR + "%")  // 2) 그 다음 와일드카드 처리
                    .replace("_", ESCAPE_CHAR + "_");
    }

    // 검색 목록 기본 조건: 공개 수업(isListed=true) + 모집 중인 수업만
    // IN_PROGRESS(수업 진행 중)는 신규 수강생을 받지 않으므로 검색 노출에서 제외
    public static Specification<Course> isSearchable() {
        return (root, query, cb) -> cb.and(
                cb.isTrue(root.get("isListed")),
                cb.equal(root.get("status"), CourseStatus.RECRUITING)
        );
    }

    // 좌표(위도·경도)가 모두 채워진 수업만 — 거리순 정렬 대상 한정.
    // 대면 수업이라도 선생님이 지도 핀을 찍지 않아 좌표가 null이면 거리 계산이 불가능하므로 제외한다.
    public static Specification<Course> hasCoordinates() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("locationLat")),
                cb.isNotNull(root.get("locationLng"))
        );
    }

    // 학생 좌표(lat/lng)와의 거리(Haversine) 오름차순 정렬을 부여한다.
    // - 조건(WHERE)은 추가하지 않고 ORDER BY만 설정하므로 항상 null Predicate를 반환.
    // - count 쿼리에는 ORDER BY가 불필요하고 일부 DB에서 오류를 내므로 건너뛴다.
    // - acos 인자가 부동소수 오차로 1.0을 살짝 넘으면 도메인 오류가 나므로 least(1.0, ...)로 클램프.
    // - 거리 동률 시 페이지네이션 안정성을 위해 id 오름차순을 보조 정렬로 둔다.
    public static Specification<Course> orderByDistance(Double lat, Double lng) {
        return (root, query, cb) -> {
            if (lat == null || lng == null) return null;
            if (query.getResultType() != null && Long.class.equals(query.getResultType())) {
                return null; // count 쿼리
            }

            Expression<Double> latCol = root.get("locationLat");
            Expression<Double> lngCol = root.get("locationLng");

            Expression<Double> radStudentLat = cb.function("radians", Double.class, cb.literal(lat));
            Expression<Double> radStudentLng = cb.function("radians", Double.class, cb.literal(lng));
            Expression<Double> radCourseLat  = cb.function("radians", Double.class, latCol);
            Expression<Double> radCourseLng  = cb.function("radians", Double.class, lngCol);

            // cos(radStudentLat)*cos(radCourseLat)*cos(radCourseLng - radStudentLng) + sin(radStudentLat)*sin(radCourseLat)
            Expression<Double> cosTerm = cb.prod(
                    cb.prod(
                            cb.function("cos", Double.class, radStudentLat),
                            cb.function("cos", Double.class, radCourseLat)
                    ),
                    cb.function("cos", Double.class, cb.diff(radCourseLng, radStudentLng))
            );
            Expression<Double> sinTerm = cb.prod(
                    cb.function("sin", Double.class, radStudentLat),
                    cb.function("sin", Double.class, radCourseLat)
            );
            Expression<Double> clamped = cb.function("least", Double.class, cb.literal(1.0), cb.sum(cosTerm, sinTerm));
            Expression<Double> distance = cb.prod(cb.literal(6371.0), cb.function("acos", Double.class, clamped));

            query.orderBy(cb.asc(distance), cb.asc(root.get("id")));
            return null;
        };
    }
}
