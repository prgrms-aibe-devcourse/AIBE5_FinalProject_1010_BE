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

    /**
     * 1분마다 호스트 부재 세션 스윕(이전 실행 종료 후 60초).
     * ⚠️ 다중 인스턴스 수평 확장 시에는 모든 인스턴스가 동시에 돌아 같은 세션을 중복 종료(종료 이벤트 중복 송신)할 수 있다.
     *   그때는 ShedLock 등 분산 락을 도입할 것(현재 단일 인스턴스 기준이라 미적용).
     */
    @Scheduled(fixedDelay = 60_000L)
    public void sweepIdleSessions() {
        try {
            classroomService.autoCloseIdleSessions();
        } catch (Exception e) {
            log.warn("[classroom] 자동종료 스윕 실패", e);
        }
    }
}
