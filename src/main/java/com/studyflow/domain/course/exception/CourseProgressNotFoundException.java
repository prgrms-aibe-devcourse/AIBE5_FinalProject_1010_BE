package com.studyflow.domain.course.exception;

// 존재하지 않거나 소프트 딜리트된 수업 진도 ID로 조회할 때 발생 → 404
public class CourseProgressNotFoundException extends RuntimeException {
    public CourseProgressNotFoundException(Long progressId) {
        super("존재하지 않는 수업 진도입니다. (id: " + progressId + ")");
    }
}
