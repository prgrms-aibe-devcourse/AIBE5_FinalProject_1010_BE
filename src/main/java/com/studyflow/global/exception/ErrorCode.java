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

    // 회원
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "사용자를 찾을 수 없습니다."),

    // 선생님 인증
    VERIFICATION_ALREADY_PENDING(HttpStatus.CONFLICT, "이미 심사 중인 인증 요청이 있습니다."),

    // 수업
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "강의를 찾을 수 없습니다."),
    COURSE_NOT_PARTICIPANT(HttpStatus.FORBIDDEN, "해당 강의의 참여자가 아닙니다."),
    COURSE_ACCESS_FORBIDDEN(HttpStatus.FORBIDDEN, "강의에 대한 접근 권한이 없습니다."),
    COURSE_HAS_ACTIVE_STUDENTS(HttpStatus.BAD_REQUEST, "수강 중인 학생이 있어 수업을 삭제할 수 없습니다."),

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

    // AI 질문
    SUBJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 과목입니다."),
    AI_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "질문 기록을 찾을 수 없습니다."),
    CONVERSATION_NOT_FOUND(HttpStatus.NOT_FOUND, "대화를 찾을 수 없습니다."),
    AI_SERVICE_ERROR(HttpStatus.BAD_GATEWAY, "AI 서비스 처리 중 오류가 발생했습니다.");

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

