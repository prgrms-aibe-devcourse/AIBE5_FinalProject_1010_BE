package com.studyflow.global.exception;

import com.studyflow.domain.ai.exception.AiQuestionNotFoundException;
import com.studyflow.domain.ai.exception.AiServiceException;
import com.studyflow.domain.ai.exception.ConversationNotFoundException;
import com.studyflow.domain.enrollment.exception.*;
import com.studyflow.domain.student.exception.StudentProfileNotFoundException;
import com.studyflow.domain.qna.exception.QnaAnswerNotFoundException;
import com.studyflow.domain.qna.exception.QnaForbiddenException;
import com.studyflow.domain.qna.exception.QnaInvalidStateException;
import com.studyflow.domain.qna.exception.QnaQuestionNotFoundException;
import com.studyflow.domain.classroom.exception.ClassroomForbiddenException;
import com.studyflow.domain.classroom.exception.ClassroomNotOpenException;
import com.studyflow.domain.classroom.exception.ClassroomSessionNotFoundException;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.auth.exception.*;
import com.studyflow.domain.course.exception.CourseHasActiveStudentsException;
import com.studyflow.domain.course.exception.CourseAccessForbiddenException;
import com.studyflow.domain.course.exception.CourseNoticeNotFoundException;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.exception.CoursePostCommentNotFoundException;
import com.studyflow.domain.course.exception.CoursePostNotFoundException;
import com.studyflow.domain.course.exception.NotCourseParticipantException;
import com.studyflow.domain.teacher.exception.InvalidVerificationFileException;
import com.studyflow.domain.teacher.exception.TeacherProfileNotFoundException;
import com.studyflow.domain.teacher.exception.VerificationAlreadyPendingException;
import com.studyflow.domain.admin.exception.StatisticsDateNotPastException;
import com.studyflow.domain.admin.exception.VerificationNotFoundException;
import com.studyflow.domain.admin.exception.VerificationNotPendingException;
import com.studyflow.domain.user.exception.DeleteAdminException;
import com.studyflow.domain.user.exception.InvalidUserUpdateException;
import com.studyflow.domain.user.exception.UserNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

    // @Validated + @RequestParam 검증 실패 (400) — ConstraintViolationException
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> v : ex.getConstraintViolations()) {
            String path = v.getPropertyPath().toString();
            String field = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            errors.put(field, v.getMessage());
        }
        Map<String, Object> body = ErrorCode.VALIDATION_ERROR.toBody(null);
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // @RequestParam enum 바인딩 실패 (400) — 예: role=INVALID
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Class<?> requiredType = ex.getRequiredType();
        String message = (requiredType != null && requiredType.isEnum())
                ? "'" + ex.getValue() + "'은(는) 올바르지 않은 값입니다."
                : "'" + ex.getName() + "' 파라미터의 형식이 올바르지 않습니다.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorCode.VALIDATION_ERROR.toBody(message));
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

    // 토큰 재발급에 필요한 refresh token이 Redis에서 조회되지 않는 경우 (401)
    @ExceptionHandler(RefreshTokenNotInRedisException.class)
    public ResponseEntity<Map<String, Object>> handleRefreshTokenNotInRedisException(RefreshTokenNotInRedisException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // 사용자를 찾을 수 없는 경우 (404)
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    // 관리자 회원탈퇴를 시도하는 경우 (403)
    @ExceptionHandler(DeleteAdminException.class)
    public ResponseEntity<Map<String, Object>> handleDeleteAdminException(DeleteAdminException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ── 회원정보 관련 예외 처리 ──────────────────────

    // 회원정보 수정 request가 유효하지 않은 경우
    @ExceptionHandler(InvalidUserUpdateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidUserUpdateException(InvalidUserUpdateException ex) {
        Map<String, Object> body = ex.getErrorCode().toBody(ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // 프로필 조회 및 수정 관련 인증 및 권한 이슈
    @ExceptionHandler(ProfileAuthInfoException.class)
    public ResponseEntity<Map<String, Object>> handleProfileAuthInfoException(ProfileAuthInfoException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Map<String, Object> body = errorCode.toBody(ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
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

    // ── 학생 도메인 예외 처리 ──────────────────────

    // 존재하지 않는 학생 프로필 조회 (404)
    @ExceptionHandler(StudentProfileNotFoundException.class)
    public ResponseEntity<String> handleStudentProfileNotFound(StudentProfileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // ── 선생님 도메인 예외 처리 ──────────────────────

    // 존재하지 않는 선생님 프로필 조회 (404)
    @ExceptionHandler(TeacherProfileNotFoundException.class)
    public ResponseEntity<String> handleTeacherProfileNotFound(TeacherProfileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // 이미 심사 중인 인증 요청이 있을 때 중복 요청 시도 (409)
    @ExceptionHandler(VerificationAlreadyPendingException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationAlreadyPending(VerificationAlreadyPendingException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 수강 중인 학생이 있는 수업 삭제 시도 (400)
    @ExceptionHandler(CourseHasActiveStudentsException.class)
    public ResponseEntity<Map<String, Object>> handleCourseHasActiveStudents(CourseHasActiveStudentsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorCode.COURSE_HAS_ACTIVE_STUDENTS.toBody(ex.getMessage()));
    }

    // 선생님 인증 파일이 유효하지 않은 경우 (존재하지 않는 파일(404), 본인 파일이 아님(403))
    @ExceptionHandler(InvalidVerificationFileException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidVerificationFileException(InvalidVerificationFileException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Map<String, Object> body = errorCode.toBody(ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // 통계 조회 날짜가 과거가 아닌 경우 (400)
    @ExceptionHandler(StatisticsDateNotPastException.class)
    public ResponseEntity<Map<String, Object>> handleStatisticsDateNotPast(StatisticsDateNotPastException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 선생님 인증 요청을 찾을 수 없는 경우 (404)
    @ExceptionHandler(VerificationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationNotFoundException(VerificationNotFoundException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Map<String, Object> body = errorCode.toBody(ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // 이미 처리된 인증 요청에 수락/거절 시도 (400)
    @ExceptionHandler(VerificationNotPendingException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationNotPendingException(VerificationNotPendingException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Map<String, Object> body = errorCode.toBody(ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // ── 수강 신청 도메인 예외 처리 ──────────────────────

    @ExceptionHandler(CourseNotRecruitingException.class)
    public ResponseEntity<Map<String, Object>> handleCourseNotRecruiting(CourseNotRecruitingException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorCode.COURSE_NOT_RECRUITING.toBody(ex.getMessage()));
    }

    @ExceptionHandler(AlreadyEnrolledException.class)
    public ResponseEntity<Map<String, Object>> handleAlreadyEnrolled(AlreadyEnrolledException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorCode.ALREADY_ENROLLED.toBody(ex.getMessage()));
    }

    @ExceptionHandler(EnrollmentRequestAlreadyPendingException.class)
    public ResponseEntity<Map<String, Object>> handleEnrollmentRequestAlreadyPending(EnrollmentRequestAlreadyPendingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorCode.ENROLLMENT_REQUEST_ALREADY_PENDING.toBody(ex.getMessage()));
    }

    @ExceptionHandler(SelfEnrollmentException.class)
    public ResponseEntity<Map<String, Object>> handleSelfEnrollment(SelfEnrollmentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorCode.SELF_ENROLLMENT.toBody(ex.getMessage()));
    }

    @ExceptionHandler(EnrollmentRequestCancelException.class)
    public ResponseEntity<Map<String, Object>> handleEnrollmentRequestCancelException(
            EnrollmentRequestCancelException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Map<String, Object> body = errorCode.toBody(ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    @ExceptionHandler(ProcessEnrollmentRequestException.class)
    public ResponseEntity<Map<String, Object>> handleProcessEnrollmentRequestException(
            ProcessEnrollmentRequestException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        Map<String, Object> body = errorCode.toBody(ex.getMessage());
        return ResponseEntity.status(errorCode.getStatus()).body(body);
    }

    // ── AI 질문 도메인 예외 처리 ──────────────────────

    // 존재하지 않는 과목으로 질문 시도 (404)
    @ExceptionHandler(SubjectNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSubjectNotFound(SubjectNotFoundException ex) {
        ErrorCode errorCode = ErrorCode.SUBJECT_NOT_FOUND;
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 존재하지 않거나 본인 소유가 아닌 질문 기록 조회 (404)
    @ExceptionHandler(AiQuestionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleAiQuestionNotFound(AiQuestionNotFoundException ex) {
        ErrorCode errorCode = ErrorCode.AI_QUESTION_NOT_FOUND;
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 존재하지 않거나 본인 소유가 아닌 대화 조회/이어쓰기/삭제 (404)
    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleConversationNotFound(ConversationNotFoundException ex) {
        ErrorCode errorCode = ErrorCode.CONVERSATION_NOT_FOUND;
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 외부 AI(OpenAI) 호출 실패 (502) — 우리 서버가 아닌 외부 의존 서비스 문제
    @ExceptionHandler(AiServiceException.class)
    public ResponseEntity<Map<String, Object>> handleAiService(AiServiceException ex) {
        ErrorCode errorCode = ErrorCode.AI_SERVICE_ERROR;
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // ── QnA 질문게시판 도메인 예외 처리 ──────────────────────

    // 존재하지 않는 질문 (404)
    @ExceptionHandler(QnaQuestionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleQnaQuestionNotFound(QnaQuestionNotFoundException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 존재하지 않는 답변 (404)
    @ExceptionHandler(QnaAnswerNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleQnaAnswerNotFound(QnaAnswerNotFoundException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 본인 글이 아니거나 채택 권한이 없는 경우 (403)
    @ExceptionHandler(QnaForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleQnaForbidden(QnaForbiddenException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 이미 채택된 질문에 재채택 시도 등 상태 위배 (코드별 상태)
    @ExceptionHandler(QnaInvalidStateException.class)
    public ResponseEntity<Map<String, Object>> handleQnaInvalidState(QnaInvalidStateException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }
    // ── 강의실(화상수업) ──

    // 강의실 세션을 찾을 수 없음 (404)
    @ExceptionHandler(ClassroomSessionNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleClassroomNotFound(ClassroomSessionNotFoundException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 담당 선생님/수강생이 아닌 사용자의 강의실 접근 (403)
    @ExceptionHandler(ClassroomForbiddenException.class)
    public ResponseEntity<Map<String, Object>> handleClassroomForbidden(ClassroomForbiddenException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 이미 종료된 강의실에 참가/종료 시도 등 상태 위배 (400)
    @ExceptionHandler(ClassroomNotOpenException.class)
    public ResponseEntity<Map<String, Object>> handleClassroomNotOpen(ClassroomNotOpenException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus()).body(errorCode.toBody(ex.getMessage()));
    }

    // 기타 예외는 필요에 따라 추가 처리

    // 헬퍼 메서드는 ErrorCode enum 내부의 toBody 메서드를 사용하도록 이동했습니다.
}
