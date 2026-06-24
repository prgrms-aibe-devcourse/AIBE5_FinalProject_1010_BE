package com.studyflow.domain.classroom.dto.response;

import java.time.LocalDateTime;

/**
 * 메인 홈 "실시간 강의중" 카드 응답 — 지금 OPEN 상태인 강의실 1건.
 *
 * <p>비로그인 포함 공개. participantCount는 LiveKit 실시간 시청자 수가 아니라
 * 강의실에 입장 기록이 있는 참가자 수(classroom_participant 행 수)다.</p>
 */
public record LiveClassroomResponse(
        Long sessionId,
        Long courseId,
        String courseTitle,
        String subjectName,
        Long teacherProfileId,
        String teacherName,
        String teacherImageUrl,   // nullable
        int participantCount,
        LocalDateTime startedAt
) {
}
