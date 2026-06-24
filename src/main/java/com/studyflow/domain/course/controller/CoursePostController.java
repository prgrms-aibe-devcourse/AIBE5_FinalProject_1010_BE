package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.comment.CoursePostCommentCreateRequest;
import com.studyflow.domain.course.dto.comment.CoursePostCommentResponse;
import com.studyflow.domain.course.dto.comment.CoursePostCommentUpdateRequest;
import com.studyflow.domain.course.dto.post.CoursePostCreateRequest;
import com.studyflow.domain.course.dto.post.CoursePostDetailResponse;
import com.studyflow.domain.course.dto.post.CoursePostSummaryResponse;
import com.studyflow.domain.course.dto.post.CoursePostUpdateRequest;
import com.studyflow.domain.course.service.CoursePostService;
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

// 수업별 자유 게시판 + 댓글 API
// 수정·삭제는 본인 또는 담당 선생님 가능 (서비스 레이어에서 검증)
@RestController
@RequestMapping("/api/v1/courses/{courseId}/posts")
@RequiredArgsConstructor
public class CoursePostController {

    private final CoursePostService postService;

    // 게시글 목록 — 최신순, 페이징
    @GetMapping
    public ResponseEntity<Page<CoursePostSummaryResponse>> getPosts(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(courseId, userId, pageable));
    }

    // 게시글 상세 조회 (조회수 1 증가)
    @GetMapping("/{postId}")
    public ResponseEntity<CoursePostDetailResponse> getPost(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(postService.getPost(courseId, postId, userId));
    }

    // 게시글 작성
    @PostMapping
    public ResponseEntity<CoursePostDetailResponse> createPost(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CoursePostCreateRequest request) {
        CoursePostDetailResponse response = postService.createPost(courseId, userId, request);
        URI location = URI.create("/api/v1/courses/" + courseId + "/posts/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    // 게시글 수정 (본인만)
    @PutMapping("/{postId}")
    public ResponseEntity<CoursePostDetailResponse> updatePost(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CoursePostUpdateRequest request) {
        return ResponseEntity.ok(postService.updatePost(courseId, postId, userId, request));
    }

    // 게시글 삭제 (본인 또는 선생님)
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId) {
        postService.deletePost(courseId, postId, userId);
        return ResponseEntity.noContent().build();
    }

    // ── 댓글 ──────────────────────────────────────

    // 댓글 작성
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CoursePostCommentResponse> createComment(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CoursePostCommentCreateRequest request) {
        CoursePostCommentResponse response = postService.createComment(courseId, postId, userId, request);
        URI location = URI.create("/api/v1/courses/" + courseId + "/posts/" + postId + "/comments/" + response.getId());
        return ResponseEntity.created(location).body(response);
    }

    // 댓글 수정 (본인만)
    @PutMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<CoursePostCommentResponse> updateComment(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CoursePostCommentUpdateRequest request) {
        return ResponseEntity.ok(postService.updateComment(courseId, postId, commentId, userId, request));
    }

    // 댓글 삭제 (본인 또는 선생님)
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long courseId,
            @PathVariable Long postId,
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long userId) {
        postService.deleteComment(courseId, postId, commentId, userId);
        return ResponseEntity.noContent().build();
    }
}
