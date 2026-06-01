package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.search.CourseCardResponse;
import com.studyflow.domain.course.dto.search.CourseSearchRequest;
import com.studyflow.domain.course.service.CourseSearchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 수업 찾기 검색 API — 비로그인 포함 전체 공개
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseSearchController {

    private final CourseSearchService courseSearchService;

    // 수업 검색 — 필터 조건은 쿼리 파라미터로 전달, 미입력 시 전체 조회
    // 예시: GET /api/v1/courses?keyword=수학&subjectId=1&targetGrades=HIGH_1,HIGH_2&minPrice=0&maxPrice=50000&sort=LATEST&page=0&size=12
    // @ModelAttribute: 쿼리 파라미터를 CourseSearchRequest 객체로 자동 바인딩
    // @Valid: CourseSearchRequest의 @Min, @AssertTrue 유효성 검사 실행
    // @PageableDefault: 기본 페이지 크기 12 (page, size는 쿼리 파라미터로 오버라이드 가능)
    @GetMapping
    public ResponseEntity<Page<CourseCardResponse>> searchCourses(
            @Valid @ModelAttribute CourseSearchRequest request,
            @PageableDefault(size = 12) Pageable pageable) {
        return ResponseEntity.ok(courseSearchService.searchCourses(request, pageable));
    }
}
