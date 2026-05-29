package com.studyflow.domain.course.exception;

// 존재하지 않는 수업 ID로 조회할 때 발생 → 404
public class CourseNotFoundException extends RuntimeException {
    public CourseNotFoundException(Long courseId) {
        super("존재하지 않는 수업입니다. (id: " + courseId + ")");
    }
}
