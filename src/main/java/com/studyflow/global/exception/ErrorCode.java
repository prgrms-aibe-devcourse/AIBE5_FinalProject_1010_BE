package com.studyflow.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token."),

    // 수업
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Course not found."),
    COURSE_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "Not a participant of this course."),
    COURSE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied for this course operation."),

    // 공지사항
    COURSE_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "Course notice not found."),

    // 자유 게시판
    COURSE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "Course post not found."),

    // 댓글
    COURSE_POST_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Course post comment not found.");

    private final HttpStatus status;
    private final String message;
}

