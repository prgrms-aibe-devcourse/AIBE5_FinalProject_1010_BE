package com.studyflow.domain.course.exception;

import com.studyflow.global.exception.ErrorCode;

// 이미 종료된 수업을 다시 종료하려 할 때 발생 → 400
public class CourseAlreadyClosedException extends RuntimeException {
    public CourseAlreadyClosedException() {
        super(ErrorCode.COURSE_ALREADY_CLOSED.getMessage());
    }
}
