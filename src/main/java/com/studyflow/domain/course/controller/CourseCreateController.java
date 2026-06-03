package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.create.CourseCreateRequest;
import com.studyflow.domain.course.dto.create.CourseCreateResponse;
import com.studyflow.domain.course.service.CourseCreateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 수업 등록 API — 선생님(TEACHER) 전용 (SecurityConfig hasRole 설정)
@Tag(name = "수업 등록", description = "선생님 수업 등록 API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseCreateController {

    private final CourseCreateService courseCreateService;

    @Operation(summary = "수업 등록", description = "선생님 전용. 새 수업을 등록하면 상태가 RECRUITING으로 생성됩니다.")
    @PostMapping
    public ResponseEntity<CourseCreateResponse> createCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseCreateService.createCourse(userId, request));
    }
}
