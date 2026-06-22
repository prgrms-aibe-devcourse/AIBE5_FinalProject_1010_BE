package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.search.CourseCardResponse;
import com.studyflow.domain.course.dto.search.CourseSearchRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseSort;
import com.studyflow.domain.course.enums.TeachingMode;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.course.specification.CourseSpecification;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CourseSearchService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;

    public Page<CourseCardResponse> searchCourses(CourseSearchRequest request, Pageable pageable) {

        CourseSort sortType = request.getSort() != null ? request.getSort() : CourseSort.LATEST;

        // 거리순 정렬은 학생 좌표가 둘 다 있을 때만 활성화 (한쪽만 있으면 DTO 검증에서 이미 차단됨).
        // 좌표가 없으면 최신순으로 안전하게 폴백한다.
        boolean distanceSort = sortType == CourseSort.DISTANCE
                && request.getStudentLat() != null && request.getStudentLng() != null;

        // 1단계: 필터 조건 조합
        // isSearchable()이 항상 non-null이므로 시작점으로 사용
        // 각 조건이 null이면 해당 Specification이 null을 반환해 자동으로 무시됨
        // 거리순일 때는 위치가 있는 대면(OFFLINE) 수업만 의미가 있으므로 teachingMode를 OFFLINE으로 강제
        TeachingMode teachingMode = distanceSort ? TeachingMode.OFFLINE : request.getTeachingMode();

        Specification<Course> spec = CourseSpecification.isSearchable()
                .and(CourseSpecification.hasKeyword(request.getKeyword()))
                .and(CourseSpecification.hasSubjects(request.getSubjectIds()))
                .and(CourseSpecification.hasTargetGrades(request.getTargetGrades()))
                .and(CourseSpecification.hasTeachingMode(teachingMode))
                .and(CourseSpecification.hasRegions(request.getRegions()))
                .and(CourseSpecification.hasMinPrice(request.getMinPrice()))
                .and(CourseSpecification.hasMaxPrice(request.getMaxPrice()))
                .and(CourseSpecification.hasMinGroupSize(request.getMinGroupSize()))
                .and(CourseSpecification.hasMaxGroupSize(request.getMaxGroupSize()));

        // 거리순이면 좌표 있는 수업만 + Haversine 거리 오름차순 정렬을 Specification에서 직접 부여
        if (distanceSort) {
            spec = spec
                    .and(CourseSpecification.hasCoordinates())
                    .and(CourseSpecification.orderByDistance(request.getStudentLat(), request.getStudentLng()));
        }

        // 2단계: 정렬 기준 생성 후 Pageable에 반영
        // 거리순은 Specification의 orderBy로 정렬하므로 Pageable에는 정렬을 지정하지 않는다(unsorted).
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                distanceSort ? Sort.unsorted() : buildSort(sortType)
        );

        // 3단계: 필터 + 정렬 + 페이지네이션으로 수업 목록 조회
        // @EntityGraph로 teacherProfile, user, subject 함께 페치 (N+1 방지)
        Page<Course> courses = courseRepository.findAll(spec, sortedPageable);

        // 4단계: 조회된 수업 ID 목록 추출
        List<Long> courseIds = courses.getContent().stream()
                .map(Course::getId)
                .toList();

        // 결과가 없으면 빈 페이지 즉시 반환
        if (courseIds.isEmpty()) {
            return Page.empty(sortedPageable);
        }

        // 5단계: 수강생 수 일괄 조회 (수업마다 쿼리를 따로 날리는 N+1 방지)
        Map<Long, Long> enrollmentCounts = buildEnrollmentCountMap(courseIds);

        // 6단계: Course → CourseCardResponse 변환
        // 거리순이면 카드에 표시할 거리(km)를 현재 페이지(최대 size개)에 대해서만 계산
        return courses.map(course -> CourseCardResponse.of(
                course,
                enrollmentCounts.getOrDefault(course.getId(), 0L), // 수강생 없는 수업은 0으로 처리
                distanceSort
                        ? distanceKm(request.getStudentLat(), request.getStudentLng(),
                                     course.getLocationLat(), course.getLocationLng())
                        : null
        ));
    }

    // request.sort 값을 Spring Data Sort 객체로 변환
    private Sort buildSort(CourseSort sort) {
        return switch (sort) {
            case PRICE_ASC  -> Sort.by(Sort.Direction.ASC,  "pricePerSession");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "pricePerSession");
            case OLDEST     -> Sort.by(Sort.Direction.ASC,  "createdAt");
            case LATEST     -> Sort.by(Sort.Direction.DESC, "createdAt");
            // 거리순(DISTANCE)은 좌표가 있으면 호출부(distanceSort 분기)에서 Specification orderBy로 처리되어
            // 이 메서드를 타지 않는다. 또한 좌표 없는 DISTANCE는 DTO 검증(@AssertTrue)에서 이미 차단된다.
            // 그럼에도 도달한다면(좌표 누락 등 예외 상황) 안전하게 최신순으로 폴백한다.
            case DISTANCE   -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    // 두 좌표 사이 거리를 Haversine 공식으로 계산해 소수 첫째 자리 km로 반환 (표시용).
    //
    // ⚠️ 의도적 공식 분리:
    //   - DB 정렬(CourseSpecification.orderByDistance)은 구면 코사인 법칙(SLoC)을 사용한다.
    //     Criteria로 표현하기 단순하고, 정렬은 "상대적 순서"만 맞으면 되기 때문.
    //   - 화면 표시값(이 메서드)은 근거리에서 부동소수 오차가 작은 Haversine을 사용한다.
    //   두 공식은 수학적으로 동치이며 정렬 결과도 동일하다. 표시 거리에서 수 km 이내 미세한
    //   차이가 날 수 있으나 사용자 경험에 영향이 없어 의도적으로 분리해 둔다.
    //
    // studentLat/Lng는 primitive — 호출부(distanceSort=true)에서 좌표가 non-null임이 보장된 뒤에만 호출된다.
    // 좌표가 하나라도 null이면 null 반환. 정렬은 DB에서 수행하고, 여기서는 화면 표시값만 만든다.
    private static Double distanceKm(double studentLat, double studentLng,
                                     Double courseLat, Double courseLng) {
        if (courseLat == null || courseLng == null) return null;
        final double earthRadiusKm = 6371.0;
        double dLat = Math.toRadians(courseLat - studentLat);
        double dLng = Math.toRadians(courseLng - studentLng);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(studentLat)) * Math.cos(Math.toRadians(courseLat))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.asin(Math.min(1.0, Math.sqrt(a)));
        return Math.round(earthRadiusKm * c * 10.0) / 10.0;
    }

    // countByCourseIdsAndStatus 결과를 courseId → 수강생 수 Map으로 변환
    private Map<Long, Long> buildEnrollmentCountMap(List<Long> courseIds) {
        Map<Long, Long> map = new HashMap<>();
        enrollmentRepository.countByCourseIdsAndStatus(courseIds, EnrollmentStatus.ACTIVE)
                .forEach(r -> map.put(r.getCourseId(), r.getCount()));
        return map;
    }
}
