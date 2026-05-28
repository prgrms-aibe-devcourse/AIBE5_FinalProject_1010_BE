package com.studyflow.domain.course.entity;

import com.studyflow.domain.constant.CourseStatus;
import com.studyflow.domain.constant.CurriculumType;
import com.studyflow.domain.constant.TargetGrade;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "course")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Course extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_profile_id", nullable = false)
    private TeacherProfile teacherProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id", nullable = false)
    private Subject subject;

    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetGrade targetGrade;

    @Column(nullable = false)
    private int maxStudents;

    @Column(nullable = false)
    private int durationMinutes;

    @Column(nullable = false)
    private int pricePerSession;

    @Column(length = 500)
    private String textbook;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CurriculumType curriculumType = CurriculumType.CUSTOM;

    @Column(columnDefinition = "TEXT")
    private String curriculumDetail;

    @Column(columnDefinition = "TEXT")
    private String availableSchedule;

    @Column(length = 100)
    private String firstClassDate;

    @Column(length = 500)
    private String thumbnailUrl;

    private LocalDate recruitDeadline;

    private LocalDate startDate;

    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CourseStatus status = CourseStatus.RECRUITING;

    @Column(nullable = false)
    private boolean isListed = true;

    @Column(nullable = false)
    private boolean isPublicAudit = false;
}
