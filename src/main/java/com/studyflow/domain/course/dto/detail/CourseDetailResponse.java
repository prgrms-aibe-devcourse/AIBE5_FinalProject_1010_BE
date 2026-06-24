package com.studyflow.domain.course.dto.detail;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.CurriculumType;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 수업 상세 페이지 응답 — 수업 정보 + 선생님 요약 + 로그인 사용자의 수강 상태(myStatus)
@Getter
@Builder
public class CourseDetailResponse {

    // ── 수업 기본 정보 ──────────────────────
    private Long id;
    private String title;
    private Long subjectId;
    private String subjectName;
    private TargetGrade targetGrade;
    private int maxStudents;
    private long currentStudents;   // ACTIVE Enrollment 수
    private int durationMinutes;
    private int pricePerSession;
    private CourseStatus status;
    private String thumbnailUrl;
    private String description;

    // ── 수업 상세 설명 ──────────────────────
    private String textbook;
    private CurriculumType curriculumType;
    private String curriculumDetail;
    private String availableSchedule;
    private String firstClassDate;

    // ── 모집 일정 ──────────────────────────
    private LocalDate recruitDeadline;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime createdAt;

    private TeachingMode teachingMode;
    private String location;
    private Double locationLat;
    private Double locationLng;

    private TeacherSummary teacher;

    // 비로그인: null / 로그인: OWNER·TEACHER·ENROLLED·PENDING·REJECTED·CANCELLED·NOT_APPLIED
    private String myStatus;

    // 수업 상세 페이지에서 보여주는 선생님 요약 카드
    @Getter
    @Builder
    public static class TeacherSummary {
        private Long teacherProfileId;
        private Long userId;
        private String name;
        private String profileImageUrl;
        private String career;
        private String major;
        private String admissionYear;
        private int naegongScore;
        private boolean verified;             // 관리자 인증 완료 여부 (User.isVerified)
        private BigDecimal totalTeachingHours;
        private long totalEnrolledStudents;  // 이 선생님의 전체 누적 수강생 수
    }

    // Course는 teacherProfile → user, subject 까지 JOIN FETCH된 상태로 전달해야 한다
    public static CourseDetailResponse of(
            Course course,
            long currentStudents,
            long totalEnrolledStudents,
            String myStatus
    ) {
        TeacherProfile tp = course.getTeacherProfile();
        User teacherUser = tp.getUser();

        return CourseDetailResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .subjectId(course.getSubject().getId())
                .subjectName(course.getSubject().getName())
                .targetGrade(course.getTargetGrade())
                .maxStudents(course.getMaxStudents())
                .currentStudents(currentStudents)
                .durationMinutes(course.getDurationMinutes())
                .pricePerSession(course.getPricePerSession())
                .status(course.getStatus())
                .thumbnailUrl(course.getThumbnailUrl())
                .description(course.getDescription())
                .textbook(course.getTextbook())
                .curriculumType(course.getCurriculumType())
                .curriculumDetail(course.getCurriculumDetail())
                .availableSchedule(course.getAvailableSchedule())
                .firstClassDate(course.getFirstClassDate())
                .recruitDeadline(course.getRecruitDeadline())
                .startDate(course.getStartDate())
                .endDate(course.getEndDate())
                .createdAt(course.getCreatedAt())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())
                .locationLat(course.getLocationLat())
                .locationLng(course.getLocationLng())
                .teacher(TeacherSummary.builder()
                        .teacherProfileId(tp.getId())
                        .userId(teacherUser.getId())
                        .name(teacherUser.getName())
                        .profileImageUrl(teacherUser.getProfileImageUrl())
                        .career(tp.getCareer())
                        .major(tp.getMajor())
                        .admissionYear(tp.getAdmissionYear())
                        .naegongScore(tp.getNaegongScore())
                        .verified(teacherUser.isVerified())
                        .totalTeachingHours(tp.getTotalTeachingHours())
                        .totalEnrolledStudents(totalEnrolledStudents)
                        .build())
                .myStatus(myStatus)
                .build();
    }
}
