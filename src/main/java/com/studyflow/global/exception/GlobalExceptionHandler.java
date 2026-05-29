package com.studyflow.global.exception;

import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.auth.exception.InvalidBirthDateException;
import com.studyflow.domain.auth.exception.InvalidGenderException;
import com.studyflow.domain.auth.exception.InvalidRoleException;
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

    /**
     * 잘못된 요청 인자에 대한 처리.
     *
     * 서비스 계층에서 클라이언트 입력 문제로 던지는 IllegalArgumentException
     * (예: 허용되지 않는 확장자/형식, 존재하지 않는 대상 등)을 500이 아닌 400으로 내려주고,
     * 사유 메시지를 함께 전달한다.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, String> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(InvalidBirthDateException.class)
    public ResponseEntity<Object> handleInvalidBirthDateException(InvalidBirthDateException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidGenderException.class)
    public ResponseEntity<Object> handleInvalidGenderException(InvalidGenderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(InvalidRoleException.class)
    public ResponseEntity<Object> handleInvalidRoleException(InvalidRoleException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    // ── 수업별 페이지 예외 처리 ──────────────────────

    // 존재하지 않는 수업 조회 (404)
    @ExceptionHandler(CourseNotFoundException.class)
    public ResponseEntity<String> handleCourseNotFound(CourseNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 존재하지 않거나 삭제된 공지 조회 (404)
    @ExceptionHandler(CourseNoticeNotFoundException.class)
    public ResponseEntity<String> handleCourseNoticeNotFound(CourseNoticeNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 존재하지 않거나 삭제된 게시글 조회 (404)
    @ExceptionHandler(CoursePostNotFoundException.class)
    public ResponseEntity<String> handleCoursePostNotFound(CoursePostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 존재하지 않거나 삭제된 댓글 조회 (404)
    @ExceptionHandler(CoursePostCommentNotFoundException.class)
    public ResponseEntity<String> handleCoursePostCommentNotFound(CoursePostCommentNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 수업 참여자(선생님·수강생)가 아닌 사용자 접근 (403)
    @ExceptionHandler(NotCourseParticipantException.class)
    public ResponseEntity<String> handleNotCourseParticipant(NotCourseParticipantException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // 선생님 전용 기능이거나 본인 게시물이 아닌 경우 (403)
    @ExceptionHandler(CourseAccessForbiddenException.class)
    public ResponseEntity<String> handleCourseAccessForbidden(CourseAccessForbiddenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // 기타 예외는 필요에 따라 추가 처리
}
