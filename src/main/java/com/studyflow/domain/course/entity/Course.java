package com.studyflow.domain.course.entity;

import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.CurriculumType;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
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

    @Enumerated(EnumType.STRING)
    @Column(length = 10, nullable = false)
    private TeachingMode teachingMode = TeachingMode.ONLINE;

    @Column(length = 300)
    private String location;       // 대면 수업 장소 주소

    private Double locationLat;    // 위도

    private Double locationLng;    // 경도

    // 수업 비공개 처리 (soft delete) — hard delete 대신 사용
    // 관련 Enrollment, ChatRoom, 게시글 등 FK 참조가 많아 hard delete 시 오류 위험
    public void close() {
        this.status = CourseStatus.CLOSED;
        this.isListed = false;
    }

    // 수업 수정 — 변경 가능한 필드만 업데이트, 소유권 확인은 서비스에서 처리
    public void update(
            Subject subject,
            String title, String description, TargetGrade targetGrade,
            int maxStudents, int durationMinutes, int pricePerSession,
            String textbook, CurriculumType curriculumType, String curriculumDetail,
            String availableSchedule, String firstClassDate, String thumbnailUrl,
            LocalDate recruitDeadline, LocalDate startDate, LocalDate endDate,
            TeachingMode teachingMode, String location, Double locationLat, Double locationLng
    ) {
        this.subject = subject;
        this.title = title;
        this.description = description;
        this.targetGrade = targetGrade;
        this.maxStudents = maxStudents;
        this.durationMinutes = durationMinutes;
        this.pricePerSession = pricePerSession;
        this.textbook = textbook;
        this.curriculumType = curriculumType;
        this.curriculumDetail = curriculumDetail;
        this.availableSchedule = availableSchedule;
        this.firstClassDate = firstClassDate;
        this.thumbnailUrl = thumbnailUrl;
        this.recruitDeadline = recruitDeadline;
        this.startDate = startDate;
        this.endDate = endDate;
        this.teachingMode = teachingMode != null ? teachingMode : TeachingMode.ONLINE;
        this.location = location;
        this.locationLat = locationLat;
        this.locationLng = locationLng;
    }

    // 수업 생성 팩토리 메서드 — 기본값: status=RECRUITING, isListed=true
    // DTO 대신 개별 값을 받아 엔티티가 DTO에 의존하지 않도록 함 (레이어 분리)
    public static Course create(
            TeacherProfile teacherProfile, Subject subject,
            String title, String description, TargetGrade targetGrade,
            int maxStudents, int durationMinutes, int pricePerSession,
            String textbook, CurriculumType curriculumType, String curriculumDetail,
            String availableSchedule, String firstClassDate, String thumbnailUrl,
            LocalDate recruitDeadline, LocalDate startDate, LocalDate endDate,
            TeachingMode teachingMode, String location, Double locationLat, Double locationLng
    ) {
        Course c = new Course();
        c.teacherProfile = teacherProfile;
        c.subject = subject;
        c.title = title;
        c.description = description;
        c.targetGrade = targetGrade;
        c.maxStudents = maxStudents;
        c.durationMinutes = durationMinutes;
        c.pricePerSession = pricePerSession;
        c.textbook = textbook;
        c.curriculumType = curriculumType;
        c.curriculumDetail = curriculumDetail;
        c.availableSchedule = availableSchedule;
        c.firstClassDate = firstClassDate;
        c.thumbnailUrl = thumbnailUrl;
        c.recruitDeadline = recruitDeadline;
        c.startDate = startDate;
        c.endDate = endDate;
        c.teachingMode = teachingMode != null ? teachingMode : TeachingMode.ONLINE;
        c.location = location;
        c.locationLat = locationLat;
        c.locationLng = locationLng;
        c.status = CourseStatus.RECRUITING;
        c.isListed = true;
        c.isPublicAudit = false;
        return c;
    }
}
