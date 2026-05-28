package com.studyflow.domain.enrollment.entity;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 수강 등록 엔티티
 *
 * EnrollmentRequest가 ACCEPTED 되면 생성된다.
 * 수강 중인 학생과 수업 간의 실제 연결을 나타낸다.
 */
@Entity
@Table(name = "enrollment")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 수강 중인 학생 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** 수강 중인 수업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    /** 이 수강으로 이어진 원본 신청 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enrollment_request_id", nullable = false)
    private EnrollmentRequest enrollmentRequest;

    /** 수강 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status = EnrollmentStatus.ACTIVE;

    /**
     * 강제 퇴장 사유.
     *
     * status가 EXPELLED일 때만 값이 존재한다.
     */
    @Column(columnDefinition = "TEXT")
    private String expelReason;

    /** 수강 등록 시각 (자동 설정) */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime enrolledAt;

    /**
     * 수업 종료 시각.
     *
     * status가 COMPLETED일 때 complete()로 설정한다.
     */
    @Column
    private LocalDateTime completedAt;

    public static Enrollment create(User user, Course course, EnrollmentRequest enrollmentRequest) {
        Enrollment enrollment = new Enrollment();
        enrollment.user = user;
        enrollment.course = course;
        enrollment.enrollmentRequest = enrollmentRequest;
        return enrollment;
    }

    public void complete() {
        this.status = EnrollmentStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();
    }

    public void expel(String reason) {
        this.status = EnrollmentStatus.EXPELLED;
        this.expelReason = reason;
    }

    public void cancel() {
        this.status = EnrollmentStatus.CANCELLED;
    }
}
