package com.studyflow.domain.classroom.entity;

import com.studyflow.domain.classroom.enums.ClassroomStatus;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 강의실 세션 — 선생님이 특정 수업(Course)에 대해 연 실시간 화상수업 방.
 *
 * <p>한 수업에는 동시에 하나의 OPEN 세션만 존재한다(서비스에서 보장).
 * 종료(close) 시 status=CLOSED, endedAt·durationSeconds가 채워진다.</p>
 */
@Entity
@Table(name = "classroom_session")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ClassroomSession extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 세션이 속한 수업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ClassroomStatus status = ClassroomStatus.OPEN;

    /** 강의실이 열린 시각 */
    @Column(nullable = false)
    private LocalDateTime startedAt;

    /** 강의실이 종료된 시각 (CLOSED일 때만 값 존재) */
    @Column
    private LocalDateTime endedAt;

    /** 진행 시간(초) — close 시 startedAt~endedAt 차이로 계산 */
    @Column
    private Long durationSeconds;

    /** 호스트(선생님)가 마지막으로 살아있다고 알린(하트비트) 시각 — 부재 자동종료 판단용 */
    @Column
    private LocalDateTime lastHostSeenAt;

    public static ClassroomSession open(Course course) {
        ClassroomSession session = new ClassroomSession();
        session.course = course;
        session.status = ClassroomStatus.OPEN;
        session.startedAt = LocalDateTime.now();
        session.lastHostSeenAt = session.startedAt; // 연 직후엔 호스트 접속으로 간주
        return session;
    }

    public boolean isOpen() {
        return this.status == ClassroomStatus.OPEN;
    }

    /** 호스트 하트비트 — "지금 접속해 있음"을 갱신 */
    public void touchHost() {
        this.lastHostSeenAt = LocalDateTime.now();
    }

    public void close() {
        this.status = ClassroomStatus.CLOSED;
        this.endedAt = LocalDateTime.now();
        this.durationSeconds = Duration.between(this.startedAt, this.endedAt).getSeconds();
    }
}
