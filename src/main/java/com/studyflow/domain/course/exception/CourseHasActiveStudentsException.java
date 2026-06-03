package com.studyflow.domain.course.exception;

import com.studyflow.global.exception.ErrorCode;

// 수강 중인 학생이 있는 수업을 삭제하려 할 때 발생 → 400
public class CourseHasActiveStudentsException extends RuntimeException {
    public CourseHasActiveStudentsException() {
        super(ErrorCode.COURSE_HAS_ACTIVE_STUDENTS.getMessage());
    }
}
