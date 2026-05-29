package com.studyflow.domain.course.exception;

// 해당 수업의 선생님도 수강생도 아닌 사용자가 접근할 때 발생
public class NotCourseParticipantException extends RuntimeException {
    public NotCourseParticipantException() {
        super("해당 수업의 참여자가 아닙니다.");
    }
}
