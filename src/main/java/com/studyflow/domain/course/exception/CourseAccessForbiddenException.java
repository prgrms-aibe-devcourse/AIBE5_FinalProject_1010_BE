package com.studyflow.domain.course.exception;

// 권한이 없는 작업(선생님 전용 또는 본인 게시물 아님)을 시도할 때 발생
public class CourseAccessForbiddenException extends RuntimeException {
    public CourseAccessForbiddenException() {
        super("해당 수업에서 권한이 없는 작업입니다.");
    }
}
