package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.CourseProgress;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {

    // 삭제되지 않은 진도 목록 — 정렬은 컨트롤러 Pageable로 전달(진도 날짜 최신순 기본)
    Page<CourseProgress> findByCourseIdAndDeletedAtIsNull(Long courseId, Pageable pageable);

    // 수업 범위 안에서 단건 조회 (삭제된 진도 제외)
    Optional<CourseProgress> findByIdAndCourseIdAndDeletedAtIsNull(Long id, Long courseId);

    // 같은 수업·같은 날짜의 진도(삭제 제외) — 하나의 수업당 날짜별 1건 유지(있으면 이어붙임)
    Optional<CourseProgress> findByCourseIdAndProgressDateAndDeletedAtIsNull(Long courseId, LocalDate progressDate);
}
