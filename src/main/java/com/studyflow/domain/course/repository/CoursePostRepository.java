package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CoursePost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CoursePostRepository extends JpaRepository<CoursePost, Long> {

    // 삭제되지 않은 게시글 목록 (페이징)
    Page<CoursePost> findByCourseIdAndDeletedAtIsNull(Long courseId, Pageable pageable);

    // 수업 범위 안에서 단건 조회 (삭제된 게시글 제외)
    Optional<CoursePost> findByIdAndCourseIdAndDeletedAtIsNull(Long id, Long courseId);

    // 동시 요청 시 lost update 방지 — DB 레벨 원자적 증가
    @Modifying
    @Query("UPDATE CoursePost p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
    void incrementViewCount(@Param("postId") Long postId);
}
