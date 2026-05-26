package com.studyflow.domain.enrollment.entity;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "enrollments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(columnDefinition = "TEXT")
    private String applyMessage;

    private String desiredSchedule;

    @Enumerated(EnumType.STRING)
    private EnrollmentStatus status = EnrollmentStatus.PENDING;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Enrollment(User student, Course course, String applyMessage, String desiredSchedule) {
        this.student = student;
        this.course = course;
        this.applyMessage = applyMessage;
        this.desiredSchedule = desiredSchedule;
    }

    public void approve() {
        this.status = EnrollmentStatus.APPROVED;
    }

    public void reject() {
        this.status = EnrollmentStatus.REJECTED;
    }
}
