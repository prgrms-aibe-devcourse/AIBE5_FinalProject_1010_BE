package com.studyflow.domain.course.repository;

import com.studyflow.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    // teacherProfile → user, subject 까지 한 번에 페치 — 수업별 페이지 진입마다 사용하므로 N+1 방지용으로 분리
    @Query("SELECT c FROM Course c " +
           "JOIN FETCH c.teacherProfile tp " +
           "JOIN FETCH tp.user " +
           "JOIN FETCH c.subject " +
           "WHERE c.id = :courseId")
    Optional<Course> findWithTeacherAndSubjectById(@Param("courseId") Long courseId);
}
