package com.studyflow.domain.assignment.exception;

// 존재하지 않는 과제 ID로 조회할 때 발생 → 404
public class AssignmentNotFoundException extends RuntimeException {
    public AssignmentNotFoundException(Long assignmentId) {
        super("존재하지 않는 과제입니다. (id: " + assignmentId + ")");
    }
}
