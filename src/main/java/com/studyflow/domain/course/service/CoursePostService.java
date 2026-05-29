package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.comment.CoursePostCommentCreateRequest;
import com.studyflow.domain.course.dto.comment.CoursePostCommentResponse;
import com.studyflow.domain.course.dto.comment.CoursePostCommentUpdateRequest;
import com.studyflow.domain.course.dto.post.CoursePostCreateRequest;
import com.studyflow.domain.course.dto.post.CoursePostDetailResponse;
import com.studyflow.domain.course.dto.post.CoursePostSummaryResponse;
import com.studyflow.domain.course.dto.post.CoursePostUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.entity.CoursePost;
import com.studyflow.domain.course.entity.CoursePostComment;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CoursePostCommentNotFoundException;
import com.studyflow.domain.course.exception.CoursePostNotFoundException;
import com.studyflow.domain.course.repository.CoursePostCommentRepository;
import com.studyflow.domain.course.repository.CoursePostRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CoursePostService {

    private final CoursePostRepository postRepository;
    private final CoursePostCommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CourseAccessValidator accessValidator;

    // 게시글 목록 — 선생님·수강생 모두 접근 가능
    @Transactional(readOnly = true)
    public Page<CoursePostSummaryResponse> getPosts(Long courseId, Long userId, Pageable pageable) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        return postRepository.findByCourseIdAndDeletedAtIsNull(courseId, pageable)
                .map(post -> {
                    long count = commentRepository.countByCoursePostIdAndDeletedAtIsNull(post.getId());
                    return CoursePostSummaryResponse.of(post, count);
                });
    }

    // 게시글 상세 조회 — 조회 시 viewCount 1 증가 (dirty checking 으로 자동 반영)
    public CoursePostDetailResponse getPost(Long courseId, Long postId, Long userId) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        CoursePost post = postRepository.findByIdAndCourseIdAndDeletedAtIsNull(postId, courseId)
                .orElseThrow(() -> new CoursePostNotFoundException(postId));
        post.incrementViewCount();

        List<CoursePostCommentResponse> comments = commentRepository
                .findByCoursePostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(CoursePostCommentResponse::from)
                .toList();

        return CoursePostDetailResponse.of(post, comments);
    }

    // 게시글 작성 — 선생님·수강생 모두 가능
    public CoursePostDetailResponse createPost(Long courseId, Long userId, CoursePostCreateRequest request) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        CoursePost post = CoursePost.create(author, course, request.getTitle(), request.getContent());
        postRepository.save(post);
        return CoursePostDetailResponse.of(post, List.of());
    }

    // 게시글 수정 — 작성자 본인만 가능
    public CoursePostDetailResponse updatePost(Long courseId, Long postId, Long userId, CoursePostUpdateRequest request) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        CoursePost post = postRepository.findByIdAndCourseIdAndDeletedAtIsNull(postId, courseId)
                .orElseThrow(() -> new CoursePostNotFoundException(postId));
        validateAuthor(post.getUser().getId(), userId);

        post.update(request.getTitle(), request.getContent());

        List<CoursePostCommentResponse> comments = commentRepository
                .findByCoursePostIdAndDeletedAtIsNullOrderByCreatedAtAsc(postId)
                .stream()
                .map(CoursePostCommentResponse::from)
                .toList();
        return CoursePostDetailResponse.of(post, comments);
    }

    // 게시글 소프트 딜리트 — 작성자 본인 또는 담당 선생님 가능
    public void deletePost(Long courseId, Long postId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        CoursePost post = postRepository.findByIdAndCourseIdAndDeletedAtIsNull(postId, courseId)
                .orElseThrow(() -> new CoursePostNotFoundException(postId));

        boolean isAuthor = post.getUser().getId().equals(userId);
        boolean isTeacher = accessValidator.isTeacher(course, userId);
        if (!isAuthor && !isTeacher) {
            throw new CourseAccessForbiddenException();
        }
        post.delete();
    }

    // ── 댓글 ──────────────────────────────────────

    // 댓글 작성 — 선생님·수강생 모두 가능
    public CoursePostCommentResponse createComment(Long courseId, Long postId, Long userId,
                                                   CoursePostCommentCreateRequest request) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        CoursePost post = postRepository.findByIdAndCourseIdAndDeletedAtIsNull(postId, courseId)
                .orElseThrow(() -> new CoursePostNotFoundException(postId));
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found: " + userId));

        CoursePostComment comment = CoursePostComment.create(post, author, request.getContent());
        return CoursePostCommentResponse.from(commentRepository.save(comment));
    }

    // 댓글 수정 — 작성자 본인만 가능
    public CoursePostCommentResponse updateComment(Long courseId, Long postId, Long commentId,
                                                   Long userId, CoursePostCommentUpdateRequest request) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        postRepository.findByIdAndCourseIdAndDeletedAtIsNull(postId, courseId)
                .orElseThrow(() -> new CoursePostNotFoundException(postId));

        CoursePostComment comment = commentRepository.findByIdAndCoursePostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new CoursePostCommentNotFoundException(commentId));
        validateAuthor(comment.getUser().getId(), userId);

        comment.update(request.getContent());
        return CoursePostCommentResponse.from(comment);
    }

    // 댓글 소프트 딜리트 — 작성자 본인 또는 담당 선생님 가능
    public void deleteComment(Long courseId, Long postId, Long commentId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        postRepository.findByIdAndCourseIdAndDeletedAtIsNull(postId, courseId)
                .orElseThrow(() -> new CoursePostNotFoundException(postId));

        CoursePostComment comment = commentRepository.findByIdAndCoursePostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new CoursePostCommentNotFoundException(commentId));

        boolean isAuthor = comment.getUser().getId().equals(userId);
        boolean isTeacher = accessValidator.isTeacher(course, userId);
        if (!isAuthor && !isTeacher) {
            throw new CourseAccessForbiddenException();
        }
        comment.delete();
    }

    // 요청자가 리소스 작성자인지 확인
    private void validateAuthor(Long resourceOwnerId, Long requesterId) {
        if (!resourceOwnerId.equals(requesterId)) {
            throw new CourseAccessForbiddenException();
        }
    }
}
