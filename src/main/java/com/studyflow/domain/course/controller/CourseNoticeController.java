package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.notice.CourseNoticeCreateRequest;
import com.studyflow.domain.course.dto.notice.CourseNoticeResponse;
import com.studyflow.domain.course.dto.notice.CourseNoticeUpdateRequest;
import com.studyflow.domain.course.service.CourseNoticeService;
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

// 수업별 공지사항 API — 작성·수정·삭제는 담당 선생님 전용 (서비스 레이어에서 검증)
@RestController
@RequestMapping("/api/v1/courses/{courseId}/notices")
@RequiredArgsConstructor
public class CourseNoticeController {

    private final CourseNoticeService noticeService;

    // 공지 목록 — 중요 공지 우선(important desc), 이후 최신순
    @GetMapping
    public ResponseEntity<Page<CourseNoticeResponse>> getNotices(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = {"important", "createdAt"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(noticeService.getNotices(courseId, userId, pageable));
    }

    // 공지 단건 조회
    @GetMapping("/{noticeId}")
    public ResponseEntity<CourseNoticeResponse> getNotice(
            @PathVariable Long courseId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(noticeService.getNotice(courseId, noticeId, userId));
    }

    // 공지 작성 (선생님 전용)
    @PostMapping
    public ResponseEntity<CourseNoticeResponse> createNotice(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseNoticeCreateRequest request) {
        CourseNoticeResponse response = noticeService.createNotice(courseId, userId, request);
        URI location = URI.create("/api/v1/courses/" + courseId + "/notices/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    // 공지 수정 (선생님 전용)
    @PutMapping("/{noticeId}")
    public ResponseEntity<CourseNoticeResponse> updateNotice(
            @PathVariable Long courseId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CourseNoticeUpdateRequest request) {
        return ResponseEntity.ok(noticeService.updateNotice(courseId, noticeId, userId, request));
    }

    // 공지 삭제 (선생님 전용)
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> deleteNotice(
            @PathVariable Long courseId,
            @PathVariable Long noticeId,
            @AuthenticationPrincipal Long userId) {
        noticeService.deleteNotice(courseId, noticeId, userId);
        return ResponseEntity.noContent().build();
    }
}
