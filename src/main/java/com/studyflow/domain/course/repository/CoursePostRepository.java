package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CoursePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CoursePostRepository extends JpaRepository<CoursePost, Long> {

    // 삭제되지 않은 게시글 목록 (페이징)
    Page<CoursePost> findByCourseIdAndDeletedAtIsNull(Long courseId, Pageable pageable);

    // 수업 범위 안에서 단건 조회 (삭제된 게시글 제외)
    Optional<CoursePost> findByIdAndCourseIdAndDeletedAtIsNull(Long id, Long courseId);
}
