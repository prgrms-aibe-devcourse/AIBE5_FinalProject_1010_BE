package com.studyflow.domain.course.exception;

// 존재하지 않거나 소프트 딜리트된 공지 ID로 조회할 때 발생 → 404
public class CourseNoticeNotFoundException extends RuntimeException {
    public CourseNoticeNotFoundException(Long noticeId) {
        super("존재하지 않는 공지사항입니다. (id: " + noticeId + ")");
    }
}
