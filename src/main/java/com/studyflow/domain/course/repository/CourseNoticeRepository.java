package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CourseNotice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseNoticeRepository extends JpaRepository<CourseNotice, Long> {

    // 삭제되지 않은 공지 목록 — 중요 공지 우선, 이후 최신순 정렬은 컨트롤러 Pageable로 전달
    Page<CourseNotice> findByCourseIdAndDeletedAtIsNull(Long courseId, Pageable pageable);

    // 수업 범위 안에서 단건 조회 (삭제된 공지는 제외)
    Optional<CourseNotice> findByIdAndCourseIdAndDeletedAtIsNull(Long id, Long courseId);
}
