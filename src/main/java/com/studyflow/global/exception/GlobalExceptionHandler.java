package com.studyflow.global.exception;

import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.auth.exception.InvalidCredentialsException;
import com.studyflow.domain.auth.exception.TermsAgreementException;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseNoticeNotFoundException;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.exception.CoursePostCommentNotFoundException;
import com.studyflow.domain.course.exception.CoursePostNotFoundException;
import com.studyflow.domain.course.exception.NotCourseParticipantException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<String> handleEmailExists(AccountAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<String> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(TermsAgreementException.class)
    public ResponseEntity<Object> handleTermsAgreementException(TermsAgreementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<String> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(CourseNoticeNotFoundException.class)
    public ResponseEntity<String> handleCourseNoticeNotFound(CourseNoticeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(CoursePostNotFoundException.class)
    public ResponseEntity<String> handleCoursePostNotFound(CoursePostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(CoursePostCommentNotFoundException.class)
    public ResponseEntity<String> handleCoursePostCommentNotFound(CoursePostCommentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 수업 참여자가 아닌 사용자 접근 (403)
    @ExceptionHandler(NotCourseParticipantException.class)
    public ResponseEntity<String> handleNotCourseParticipant(NotCourseParticipantException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // 권한 없는 수업 내 작업 시도 (403)
    @ExceptionHandler(CourseAccessForbiddenException.class)
    public ResponseEntity<String> handleCourseAccessForbidden(CourseAccessForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
