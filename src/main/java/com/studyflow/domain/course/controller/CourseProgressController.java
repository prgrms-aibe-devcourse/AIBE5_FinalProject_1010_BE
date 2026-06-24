package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.progress.CourseProgressCreateRequest;
import com.studyflow.domain.course.dto.progress.CourseProgressResponse;
import com.studyflow.domain.course.dto.progress.CourseProgressUpdateRequest;
import com.studyflow.domain.course.service.CourseProgressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

// 수업별 진도 API (이슈 #173) — 작성·수정·삭제는 담당 선생님 전용(서비스 레이어 검증), 조회는 멤버
@RestController
@RequestMapping("/api/v1/courses/{courseId}/progress")
@RequiredArgsConstructor
public class CourseProgressController {

    private final CourseProgressService progressService;

    // 진도 목록 — 진도 날짜 최신순, 같은 날짜면 작성 최신순
    @GetMapping
    public ResponseEntity<Page<CourseProgressResponse>> getProgressList(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = {"progressDate", "createdAt"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(progressService.getProgressList(courseId, userId, pageable));
    }

    // 진도 단건 조회
    @GetMapping("/{progressId}")
    public ResponseEntity<CourseProgressResponse> getProgress(
            @PathVariable Long courseId,
            @PathVariable Long progressId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(progressService.getProgress(courseId, progressId, userId));
    }

    // 진도 작성 (선생님 전용)
    @PostMapping
    public ResponseEntity<CourseProgressResponse> createProgress(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseProgressCreateRequest request) {
        CourseProgressResponse response = progressService.createProgress(courseId, userId, request);
        URI location = URI.create("/api/v1/courses/" + courseId + "/progress/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    // 진도 수정 (선생님 전용)
    @PutMapping("/{progressId}")
    public ResponseEntity<CourseProgressResponse> updateProgress(
            @PathVariable Long courseId,
            @PathVariable Long progressId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseProgressUpdateRequest request) {
        return ResponseEntity.ok(progressService.updateProgress(courseId, progressId, userId, request));
    }

    // 진도 삭제 (선생님 전용)
    @DeleteMapping("/{progressId}")
    public ResponseEntity<Void> deleteProgress(
            @PathVariable Long courseId,
            @PathVariable Long progressId,
            @AuthenticationPrincipal Long userId) {
        progressService.deleteProgress(courseId, progressId, userId);
        return ResponseEntity.noContent().build();
    }
}
