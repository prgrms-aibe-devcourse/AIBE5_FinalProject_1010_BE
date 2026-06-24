package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CoursePostComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CoursePostCommentRepository extends JpaRepository<CoursePostComment, Long> {

    // 게시글에 속한 삭제되지 않은 댓글 목록 (시간순)
    List<CoursePostComment> findByCoursePostIdAndDeletedAtIsNullOrderByCreatedAtAsc(Long coursePostId);

    // 게시글 범위 안에서 단건 조회 (삭제된 댓글 제외)
    Optional<CoursePostComment> findByIdAndCoursePostIdAndDeletedAtIsNull(Long id, Long coursePostId);

    // 게시글 목록 조회 시 댓글 수 표시용
    long countByCoursePostIdAndDeletedAtIsNull(Long coursePostId);
}
