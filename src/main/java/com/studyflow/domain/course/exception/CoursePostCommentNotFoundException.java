package com.studyflow.domain.course.exception;

// 존재하지 않거나 소프트 딜리트된 댓글 ID로 조회할 때 발생 → 404
public class CoursePostCommentNotFoundException extends RuntimeException {
    public CoursePostCommentNotFoundException(Long commentId) {
        super("존재하지 않는 댓글입니다. (id: " + commentId + ")");
    }
}
