package com.studyflow.domain.assignment.exception;

// 해당 수업에 속하지 않는 과제에 접근할 때 발생 → 403
public class AssignmentAccessForbiddenException extends RuntimeException {
    public AssignmentAccessForbiddenException() {
        super("해당 수업의 과제가 아닙니다.");
    }
}
