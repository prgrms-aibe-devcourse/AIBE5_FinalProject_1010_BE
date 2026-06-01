package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.search.CourseCardResponse;
import com.studyflow.domain.course.dto.search.CourseSearchRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseSort;
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

        // 1단계: 필터 조건 조합
        // isSearchable()이 항상 non-null이므로 시작점으로 사용
        // 각 조건이 null이면 해당 Specification이 null을 반환해 자동으로 무시됨
        Specification<Course> spec = CourseSpecification.isSearchable()
                .and(CourseSpecification.hasKeyword(request.getKeyword()))
                .and(CourseSpecification.hasSubjects(request.getSubjectIds()))
                .and(CourseSpecification.hasTargetGrades(request.getTargetGrades()))
                .and(CourseSpecification.hasMinPrice(request.getMinPrice()))
                .and(CourseSpecification.hasMaxPrice(request.getMaxPrice()));

        // 2단계: 요청의 sort 필드로 정렬 기준 생성 후 Pageable에 반영
        // 컨트롤러에서 넘어온 Pageable의 sort는 무시하고 request.sort를 우선 사용
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                buildSort(request.getSort())
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
            return Page.empty(pageable);
        }

        // 5단계: 수강생 수 일괄 조회 (수업마다 쿼리를 따로 날리는 N+1 방지)
        Map<Long, Long> enrollmentCounts = buildEnrollmentCountMap(courseIds);

        // 6단계: Course → CourseCardResponse 변환
        return courses.map(course -> CourseCardResponse.of(
                course,
                enrollmentCounts.getOrDefault(course.getId(), 0L) // 수강생 없는 수업은 0으로 처리
        ));
    }

    // request.sort 값을 Spring Data Sort 객체로 변환
    private Sort buildSort(CourseSort sort) {
        if (sort == null) return Sort.by(Sort.Direction.DESC, "createdAt");
        return switch (sort) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "pricePerSession");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "pricePerSession");
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    // countByCourseIdsAndStatus 결과(Object[])를 courseId → 수강생 수 Map으로 변환
    // Object[0] = courseId(Long), Object[1] = count(Long)
    private Map<Long, Long> buildEnrollmentCountMap(List<Long> courseIds) {
        Map<Long, Long> map = new HashMap<>();
        enrollmentRepository.countByCourseIdsAndStatus(courseIds, EnrollmentStatus.ACTIVE)
                .forEach(row -> map.put((Long) row[0], ((Number) row[1]).longValue()));
        return map;
    }
}
