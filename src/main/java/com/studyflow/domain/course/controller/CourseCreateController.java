package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.create.CourseCreateRequest;
import com.studyflow.domain.course.dto.create.CourseCreateResponse;
import com.studyflow.domain.course.dto.update.CourseUpdateRequest;
import com.studyflow.domain.course.service.CourseCreateService;
import com.studyflow.domain.course.service.CourseUpdateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// 수업 등록 / 수정 / 종료 API — 선생님(TEACHER) 전용 (SecurityConfig hasRole 설정)
@Tag(name = "수업 관리", description = "선생님 수업 등록·수정·종료 API")
@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseCreateController {

    private final CourseCreateService courseCreateService;
    private final CourseUpdateService courseUpdateService;

    // 수업 등록 — 생성 직후 status=RECRUITING
    @Operation(summary = "수업 등록", description = "선생님 전용. 새 수업을 등록하면 상태가 RECRUITING으로 생성됩니다.")
    @PostMapping
    public ResponseEntity<CourseCreateResponse> createCourse(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(courseCreateService.createCourse(userId, request));
    }

    // 수업 수정 — 본인 수업만 가능, maxStudents 외 전체 필드 전송 (PATCH)
    @Operation(summary = "수업 수정", description = "선생님 전용. 본인 수업만 수정 가능합니다.")
    @PatchMapping("/{courseId}")
    public ResponseEntity<CourseCreateResponse> updateCourse(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseUpdateRequest request
    ) {
        return ResponseEntity.ok(courseUpdateService.updateCourse(courseId, userId, request));
    }

    // 수업 종료 — CLOSED 상태로 변경, isListed=false, PENDING 신청 일괄 거절
    @Operation(summary = "수업 종료", description = "선생님 전용. 수강 중인 학생이 있으면 종료할 수 없습니다.")
    @PatchMapping("/{courseId}/close")
    public ResponseEntity<Void> closeCourse(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        courseUpdateService.closeCourse(courseId, userId);
        return ResponseEntity.noContent().build();
    }

    // 수업 삭제 — 아무도 사용하지 않은 모집중 수업만 삭제 가능 (실제 DB 삭제)
    @Operation(summary = "수업 삭제", description = "선생님 전용. 아무도 사용하지 않은 모집중 수업만 삭제할 수 있습니다. 수강 신청·게시글·공지·과제·강의실·채팅방 중 하나라도 있으면 삭제할 수 없습니다. 이미 사용된 수업은 PATCH /{courseId}/close 를 사용하세요.")
    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(
            @PathVariable Long courseId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        courseUpdateService.deleteCourse(courseId, userId);
        return ResponseEntity.noContent().build();
    }
}
