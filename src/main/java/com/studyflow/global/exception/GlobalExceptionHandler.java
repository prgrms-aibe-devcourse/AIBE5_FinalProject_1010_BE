package com.studyflow.global.exception;

import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.auth.exception.SignupRequestException;
import com.studyflow.domain.auth.exception.InvalidCredentialsException;
import com.studyflow.domain.auth.exception.SignupWithAdminException;
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
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

// 가독성을 위해 자바 및 스프링 표준 Exception을 위에, 커스텀 Exception을 아래에 정리
@RestControllerAdvice
public class GlobalExceptionHandler {
    // @Valid 어노테이션 검증에 실패한 경우 (400)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        Map<String, Object> body = ErrorCode.VALIDATION_ERROR.toBody(null);
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // request JSON 형식이 올바르지 않은 경우 (400)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex) {
        Map<String, Object> body = ErrorCode.VALIDATION_ERROR.toBody("올바르지 않은 입력 형식입니다.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
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

    // 커스텀 Exception Handling

    // ── 회원 인증 도메인(회원가입, 로그인 등) 예외 처리 ──────────────────────

    // 가입 시도하는 이메일이 이미 가입된 경우 (409)
    @ExceptionHandler(AccountAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailExists(AccountAlreadyExistsException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    // 이메일 또는 비밀번호 불일치로 로그인 실패한 경우 (401)
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(InvalidCredentialsException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 회원가입 요청이 회원가입 비즈니스 로직에 위배되는 경우 (400)
    @ExceptionHandler(SignupRequestException.class)
    public ResponseEntity<Map<String, Object>> handleSignupRequestException(SignupRequestException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 관리자로 회원가입을 시도하는 경우 (403)
    @ExceptionHandler(SignupWithAdminException.class)
    public ResponseEntity<Map<String, Object>> handleSignupWithAdminException(SignupWithAdminException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
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

    // 헬퍼 메서드는 ErrorCode enum 내부의 toBody 메서드를 사용하도록 이동했습니다.
}
