package com.studyflow.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import java.util.HashMap;
import java.util.Map;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 입력값 검증
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "입력값이 유효하지 않습니다."),

    // 인증
    AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다."),
    AUTH_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 일치하지 않습니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    EMAIL_CONFLICT(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이메일 발송에 실패했습니다."),
    EMAIL_AUTH_CODE_INVALID(HttpStatus.BAD_REQUEST, "인증 코드가 올바르지 않거나 만료되었습니다."),
    EMAIL_VERIFIED_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "이메일 인증이 완료되지 않았거나 만료되었습니다."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "비밀번호 재설정 링크가 유효하지 않거나 만료되었습니다."),
    EMAIL_SEND_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "잠시 후 다시 요청해 주세요."),
    EMAIL_SEND_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "이메일 발송 횟수를 초과했습니다. 1시간 후 다시 시도해 주세요."),
    EMAIL_VERIFY_ATTEMPT_EXCEEDED(HttpStatus.BAD_REQUEST, "인증 시도 횟수를 초과했습니다. 인증 코드를 다시 요청해 주세요."),

    // 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),
    STUDENT_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "학생 프로필을 찾을 수 없습니다."),
    TEACHER_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "선생님 프로필을 찾을 수 없습니다."),
    TEACHER_HAS_LISTED_COURSES(HttpStatus.BAD_REQUEST, "수업 찾기에 노출 중인 수업이 있어 선생님 찾기 노출을 끌 수 없습니다. 먼저 수업을 종료해주세요."),

    // 선생님 인증
    VERIFICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 심사 중인 인증 요청이 있습니다."),
    VERIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "인증 요청을 찾을 수 없습니다."),
    VERIFICATION_NOT_PENDING(HttpStatus.BAD_REQUEST, "심사 중이 아닌 인증 요청입니다."),

    // 수업
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    COURSE_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 강의의 참여자가 아닙니다."),
    COURSE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "강의에 대한 접근 권한이 없습니다."),
    COURSE_HAS_ACTIVE_STUDENTS(HttpStatus.BAD_REQUEST, "수강 중인 학생이 있어 수업을 삭제할 수 없습니다."),
    COURSE_NOT_DELETABLE(HttpStatus.CONFLICT, "아무도 사용하지 않은 모집중 수업만 삭제할 수 있습니다. 이미 사용된 수업은 PATCH /close를 이용해 종료하세요."),

    // 공지사항
    COURSE_NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의 공지를 찾을 수 없습니다."),

    // 자유 게시판
    COURSE_POST_NOT_FOUND(HttpStatus.NOT_FOUND, "강의 게시글을 찾을 수 없습니다."),

    // 댓글
    COURSE_POST_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "강의 게시글 댓글을 찾을 수 없습니다."),

    // 수강 신청
    COURSE_NOT_RECRUITING(HttpStatus.BAD_REQUEST, "모집 중이 아닌 수업입니다."),
    ALREADY_ENROLLED(HttpStatus.CONFLICT, "이미 수강 중인 수업입니다."),
    ENROLLMENT_REQUEST_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 수강 신청이 접수되어 있습니다."),
    SELF_ENROLLMENT(HttpStatus.BAD_REQUEST, "본인의 수업에는 신청할 수 없습니다."),
    ENROLLMENT_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "수강 신청 기록을 찾을 수 없습니다."),
    NOT_MY_ENROLLMENT_REQUEST(HttpStatus.FORBIDDEN, "본인의 수강 신청이 아닙니다"),
    CANNOT_CANCEL_ENROLLMENT_REQUEST(HttpStatus.BAD_REQUEST, "취소할 수 없는 상태의 수강 신청입니다."),
    NOT_MY_COURSE_ENROLLMENT_REQUEST(HttpStatus.FORBIDDEN, "본인 수업의 수강 신청이 아닙니다."),
    CANNOT_PROCESS_ENROLLMENT_REQUEST(HttpStatus.BAD_REQUEST, "처리할 수 없는 상태의 수강 신청입니다."),

    // AI 질문
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 과목입니다."),
    AI_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문 기록을 찾을 수 없습니다."),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 처리 중 오류가 발생했습니다."),

    // QnA 질문게시판
    QNA_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문을 찾을 수 없습니다."),
    QNA_ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "답변을 찾을 수 없습니다."),
    QNA_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 작업에 대한 권한이 없습니다."),
    QNA_ALREADY_RESOLVED(HttpStatus.CONFLICT, "이미 답변이 채택된 질문입니다."),

    // 강의실(화상수업)
    CLASSROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "강의실 세션을 찾을 수 없습니다."),
    CLASSROOM_PARTICIPANT_NOT_FOUND(HttpStatus.NOT_FOUND, "강의실 참가자를 찾을 수 없습니다."),
    CLASSROOM_FORBIDDEN(HttpStatus.FORBIDDEN, "강의실에 대한 권한이 없습니다."),
    CLASSROOM_NOT_OPEN(HttpStatus.BAD_REQUEST, "열려 있는 강의실이 아닙니다."),

    // 파일
    FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다."),
    NOT_MY_FILE(HttpStatus.FORBIDDEN, "본인이 업로드한 파일만 사용할 수 있습니다."),

    // 알림
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "본인의 알림이 아닙니다."),

    // 관리자 통계
    STATISTICS_DATE_NOT_PAST(HttpStatus.BAD_REQUEST, "통계는 오늘 이전 날짜만 조회할 수 있습니다.");

    private final HttpStatus status;
    private final String message;

    /**
     * ErrorCode로부터 공통 에러 바디를 생성합니다.
     * 반환되는 map에는 최소한 다음 키가 포함됩니다: code, message
     * customMessage가 null이면 ErrorCode에 설정된 기본 메시지를 사용합니다.
     */
    public Map<String, Object> toBody(String customMessage) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", this.name());
        body.put("message", customMessage != null ? customMessage : this.getMessage());
        return body;
    }
}

