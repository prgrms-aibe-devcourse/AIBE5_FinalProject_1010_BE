package com.studyflow.domain.course.exception;

// 존재하지 않거나 소프트 딜리트된 게시글 ID로 조회할 때 발생 → 404
public class CoursePostNotFoundException extends RuntimeException {
    public CoursePostNotFoundException(Long postId) {
        super("존재하지 않는 게시글입니다. (id: " + postId + ")");
    }
}
