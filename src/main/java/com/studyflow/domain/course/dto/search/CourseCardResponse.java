package com.studyflow.domain.course.dto.search;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class CourseCardResponse {

    private Long id;
    private String title;

    // 선생님 이름 (TeacherProfile → User)
    private String teacherName;

    // 선생님 프로필 사진 URL (TeacherProfile → User)
    private String teacherProfileImageUrl;

    // 과목명 (Subject)
    private String subjectName;

    private TargetGrade targetGrade;

    // 수업 방식 (ONLINE / OFFLINE) — 카드 칩 표시 + 지역 필터 맥락
    private TeachingMode teachingMode;

    // 대면 수업 장소 (전체 주소) — OFFLINE일 때만 값 존재
    private String location;

    // 회당 수업 시간(분)
    private int durationMinutes;

    // 모집 마감일 — 카드 D-day 배지용 (null이면 마감 미설정)
    private LocalDate recruitDeadline;

    // 회당 수업료 (원)
    private int pricePerSession;

    // 최대 정원
    private int maxStudents;

    // 현재 수강 중인 학생 수 (Enrollment ACTIVE 기준)
    private long currentStudents;

    private String thumbnailUrl;
    private CourseStatus status;

    // course 엔티티 + 수강생 수를 조합해 카드 응답 생성
    // teacherProfile, user, subject는 JOIN FETCH 후 전달해야 LazyInitializationException 방지
    public static CourseCardResponse of(Course course, long currentStudents) {
        return CourseCardResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .teacherName(course.getTeacherProfile().getUser().getName())
                .teacherProfileImageUrl(course.getTeacherProfile().getUser().getProfileImageUrl())
                .subjectName(course.getSubject().getName())
                .targetGrade(course.getTargetGrade())
                .teachingMode(course.getTeachingMode())
                .location(course.getLocation())
                .durationMinutes(course.getDurationMinutes())
                .recruitDeadline(course.getRecruitDeadline())
                .pricePerSession(course.getPricePerSession())
                .maxStudents(course.getMaxStudents())
                .currentStudents(currentStudents)
                .thumbnailUrl(course.getThumbnailUrl())
                .status(course.getStatus())
                .build();
    }
}
