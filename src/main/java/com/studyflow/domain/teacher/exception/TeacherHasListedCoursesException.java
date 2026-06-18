package com.studyflow.domain.teacher.exception;

import com.studyflow.global.exception.ErrorCode;

// 수업 찾기에 노출 중인(공개+모집중) 수업이 있는데 선생님 찾기 노출을 끄려 할 때 발생 → 400
public class TeacherHasListedCoursesException extends RuntimeException {
    private final ErrorCode errorCode = ErrorCode.TEACHER_HAS_LISTED_COURSES;

    public TeacherHasListedCoursesException() {
        super(ErrorCode.TEACHER_HAS_LISTED_COURSES.getMessage());
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
