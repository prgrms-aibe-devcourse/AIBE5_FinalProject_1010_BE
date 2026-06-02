package com.studyflow.domain.teacher.exception;

// 존재하지 않는 선생님 프로필 ID로 조회할 때 발생 → 404
public class TeacherProfileNotFoundException extends RuntimeException {
    public TeacherProfileNotFoundException(Long teacherProfileId) {
        super("존재하지 않는 선생님입니다. (id: " + teacherProfileId + ")");
    }
}
