package com.studyflow.domain.course.entity;

import com.studyflow.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "courses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;

    @Column(nullable = false)
    private String title;

    private String subject;
    private String targetGrade;
    private Integer pricePerSession;
    private Integer durationMinutes;
    private Integer maxStudents;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String teachingMaterial;
    private String schedule;
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    private CourseType courseType = CourseType.ONE_ON_ONE;

    @Enumerated(EnumType.STRING)
    private CourseStatus status = CourseStatus.OPEN;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    @Builder
    public Course(User teacher, String title, String subject, String targetGrade,
                  Integer pricePerSession, Integer durationMinutes, Integer maxStudents,
                  String description, CourseType courseType) {
        this.teacher = teacher;
        this.title = title;
        this.subject = subject;
        this.targetGrade = targetGrade;
        this.pricePerSession = pricePerSession;
        this.durationMinutes = durationMinutes;
        this.maxStudents = maxStudents;
        this.description = description;
        this.courseType = courseType;
    }

    public void close() {
        this.status = CourseStatus.CLOSED;
    }
}
