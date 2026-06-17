package com.studyflow.domain.course.exception;

import com.studyflow.global.exception.ErrorCode;

// 삭제 불가 조건(사용 이력 존재 또는 RECRUITING 상태 아님)에 해당할 때 발생 → 409
public class CourseNotDeletableException extends RuntimeException {
    public CourseNotDeletableException() {
        super(ErrorCode.COURSE_NOT_DELETABLE.getMessage());
    }
}
