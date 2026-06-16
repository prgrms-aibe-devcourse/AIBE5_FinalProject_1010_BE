package com.studyflow.domain.course.dto.dashboard;

import lombok.Builder;
import lombok.Getter;

// 수강생 1명의 출석 현황 — 실시간 강의실 입장 기록 기반 집계
@Getter
@Builder
public class AttendanceResponse {
    private Long userId;
    private String name;
    private String profileImageUrl;
    private long attendedCount;  // 입장한 세션 수
    private long totalSessions;  // 수업 전체 종료 세션 수
}
