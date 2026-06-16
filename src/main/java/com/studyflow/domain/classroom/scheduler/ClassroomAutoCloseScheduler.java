package com.studyflow.domain.classroom.scheduler;

import com.studyflow.domain.classroom.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 강의실 호스트 부재 자동종료 스케줄러 (이슈 #135 후속).
 *
 * <p>선생님(호스트)이 "수업 종료" 없이 사라지는 모든 경우(뒤로가기·새로고침·크롬 종료·재부팅 등)는
 * 결국 "하트비트 끊김"으로 나타난다. 1분마다 OPEN 세션을 점검해, 호스트 하트비트가
 * 5분 넘게 끊긴 방을 강제 종료하고 종료 이벤트를 브로드캐스트한다(학생들도 자동 퇴장).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClassroomAutoCloseScheduler {

    private final ClassroomService classroomService;

    /** 1분마다 호스트 부재 세션 스윕(이전 실행 종료 후 60초). */
    @Scheduled(fixedDelay = 60_000L)
    public void sweepIdleSessions() {
        try {
            classroomService.autoCloseIdleSessions();
        } catch (Exception e) {
            log.warn("[classroom] 자동종료 스윕 실패", e);
        }
    }
}
