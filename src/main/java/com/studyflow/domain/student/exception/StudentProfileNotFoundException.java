package com.studyflow.domain.student.exception;

public class StudentProfileNotFoundException extends RuntimeException {

    public static StudentProfileNotFoundException ofUserId(Long userId) {
        return new StudentProfileNotFoundException("존재하지 않는 학생 프로필입니다. (userId: " + userId + ")");
    }

    private StudentProfileNotFoundException(String message) {
        super(message);
    }
}
