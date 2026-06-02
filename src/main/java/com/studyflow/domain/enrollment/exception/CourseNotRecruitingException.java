package com.studyflow.domain.enrollment.exception;

import com.studyflow.global.exception.ErrorCode;

// 모집 중(RECRUITING)이 아닌 수업에 신청할 때 발생 → 400
public class CourseNotRecruitingException extends RuntimeException {
    public CourseNotRecruitingException() {
        super(ErrorCode.COURSE_NOT_RECRUITING.getMessage());
    }
}
