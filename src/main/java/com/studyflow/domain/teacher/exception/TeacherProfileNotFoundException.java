package com.studyflow.domain.teacher.exception;

// 존재하지 않는 선생님 프로필 조회 시 발생 → 404
public class TeacherProfileNotFoundException extends RuntimeException {

    // teacherProfile PK로 조회할 때
    public TeacherProfileNotFoundException(Long teacherProfileId) {
        super("존재하지 않는 선생님입니다. (teacherProfileId: " + teacherProfileId + ")");
    }

    // userId(로그인 사용자 ID)로 조회할 때 — 수업 생성 시 선생님 프로필 확인용
    public static TeacherProfileNotFoundException ofUserId(Long userId) {
        return new TeacherProfileNotFoundException("존재하지 않는 선생님입니다. (userId: " + userId + ")");
    }

    private TeacherProfileNotFoundException(String message) {
        super(message);
    }
}
