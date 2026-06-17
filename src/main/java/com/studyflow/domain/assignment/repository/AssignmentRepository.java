package com.studyflow.domain.assignment.repository;

import com.studyflow.domain.assignment.entity.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByCourseIdOrderByCreatedAtDesc(Long courseId);

    // 수업 삭제 가능 여부 확인 — 과제가 한 건이라도 있으면 삭제 불가
    boolean existsByCourseId(Long courseId);
}
