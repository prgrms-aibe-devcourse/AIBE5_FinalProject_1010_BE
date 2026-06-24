package com.studyflow.domain.enrollment.entity;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 수강 신청 엔티티
 *
 * 학생이 특정 수업에 신청할 때 생성된다.
 * 선생님이 수락하면 status가 ACCEPTED로 변경되고 Enrollment가 생성된다.
 */
@Entity
@Table(
        name = "enrollment_request",
        // (user_id, course_id, status) 조합에 유니크 제약 — 동시 요청에서 PENDING 중복 신청을 DB 레벨에서 차단
        uniqueConstraints = @UniqueConstraint(
                name = "uk_enrollment_request_user_course_status",
                columnNames = {"user_id", "course_id", "status"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EnrollmentRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 신청 대상 수업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** 신청한 학생 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 자기소개 (학년, 현재 수준 등) */
    @Column(columnDefinition = "TEXT")
    private String introduction;

    /** 학습 목표 (예: 수능 1등급, 내신 90점) */
    @Column(columnDefinition = "TEXT")
    private String goal;

    /** 희망 수업 요일·시간대 메모 */
    @Column(columnDefinition = "TEXT")
    private String preferredScheduleNote;

    /** 첫 수업 희망 시기 (예: 즉시, 다음 주부터) */
    @Column(length = 100)
    private String preferredStart;

    /** 선생님께 전달할 자유 메시지 */
    @Column(columnDefinition = "TEXT")
    private String message;

    /** 신청 처리 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentRequestStatus status = EnrollmentRequestStatus.PENDING;

    // 수강 신청 취소 — PENDING 상태에서만 호출 가능 (서비스 레이어에서 검증 후 호출)
    public void cancel() {
        this.status = EnrollmentRequestStatus.CANCELLED;
    }

    // 수강 신청 수락 — PENDING 상태에서만 호출 가능 (서비스 레이어에서 검증 후 호출)
    public void accept() {
        this.status = EnrollmentRequestStatus.ACCEPTED;
    }

    // 수강 신청 거절 — PENDING 상태에서만 호출 가능 (서비스 레이어에서 검증 후 호출)
    public void reject() {
        this.status = EnrollmentRequestStatus.REJECTED;
    }

    // 수강 신청 생성 — status 기본값 PENDING, 선생님 수락 시 ACCEPTED로 변경 후 Enrollment 생성
    // DTO 대신 개별 값을 받아 엔티티가 DTO에 의존하지 않도록 함 (레이어 분리)
    public static EnrollmentRequest create(
            Course course, User user,
            String introduction, String goal,
            String preferredScheduleNote, String preferredStart,
            String message
    ) {
        EnrollmentRequest e = new EnrollmentRequest();
        e.course = course;
        e.user = user;
        e.introduction = introduction;
        e.goal = goal;
        e.preferredScheduleNote = preferredScheduleNote;
        e.preferredStart = preferredStart;
        e.message = message;
        return e;
    }
}
