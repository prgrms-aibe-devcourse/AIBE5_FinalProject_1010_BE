package com.studyflow.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    INVALID_INPUT(HttpStatus.BAD_REQUEST, "Invalid input."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Authentication required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Access denied."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "Resource not found."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error."),

    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Invalid token."),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "Expired token."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "Invalid password."),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "User not found."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email already in use."),
    TEACHER_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Teacher not verified."),

    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Course not found."),
    COURSE_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "Course already closed."),

    ENROLLMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "Enrollment not found."),
    ENROLLMENT_ALREADY_EXISTS(HttpStatus.CONFLICT, "Already enrolled."),

    CLASSROOM_NOT_OPEN(HttpStatus.BAD_REQUEST, "Classroom is not open."),
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Classroom not found."),

    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "Question not found."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "Answer not found."),
    ANSWER_ALREADY_ADOPTED(HttpStatus.BAD_REQUEST, "Answer already adopted."),

    CHAT_ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Chat room not found."),

    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "File upload failed."),
    INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "Unsupported file type.");

    private final HttpStatus status;
    private final String message;
}
