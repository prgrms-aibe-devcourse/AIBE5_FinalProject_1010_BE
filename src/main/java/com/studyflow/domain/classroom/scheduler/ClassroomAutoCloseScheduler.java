package com.studyflow.domain.classroom.scheduler;

import com.studyflow.domain.classroom.service.ClassroomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
     *
     * <p>멀티 인스턴스(EC2 2대)에서 모든 인스턴스가 동시에 돌면 같은 세션을 중복 종료(종료 이벤트 중복 송신)할 수
     * 있어 ShedLock 분산 락을 적용한다. 한 틱은 한 인스턴스만 실행한다.
     * lockAtMostFor: 실행 노드가 죽어도 락이 5분 뒤 자동 해제(데드락 방지).
     * lockAtLeastFor: 실행이 매우 빨라도 30초간 락 유지 → 클럭 스큐로 인한 즉시 중복 실행 방지.</p>
     */
    @Scheduled(fixedDelay = 60_000L)
    @SchedulerLock(name = "classroom-autoclose-sweep", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void sweepIdleSessions() {
        try {
            classroomService.autoCloseIdleSessions();
        } catch (Exception e) {
            log.warn("[classroom] 자동종료 스윕 실패", e);
        }
    }
}
