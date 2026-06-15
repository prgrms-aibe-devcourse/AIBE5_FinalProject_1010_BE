package com.studyflow.domain.course.dto.dashboard;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

// 수업별 페이지 상단 정보 — 수업 기본 정보 + 선생님 요약 + 현재 수강 인원
@Getter
@Builder
public class CourseDashboardResponse {

    // 수업 정보
    private Long courseId;
    private String title;
    private String subjectName;
    private String targetGrade;
    private String status;
    private int maxStudents;
    private long enrolledCount;

    // 담당 선생님 요약
    private Long teacherUserId;
    private Long teacherProfileId;
    private String teacherName;
    private String teacherProfileImageUrl;

    private int teacherNaegongScore;

    public static CourseDashboardResponse of(Course course, long enrolledCount) {
        TeacherProfile tp = course.getTeacherProfile();
        User teacher = tp.getUser();

        return CourseDashboardResponse.builder()
                .courseId(course.getId())
                .title(course.getTitle())
                .subjectName(course.getSubject().getName())
                .targetGrade(course.getTargetGrade().name())
                .status(course.getStatus().name())
                .maxStudents(course.getMaxStudents())
                .enrolledCount(enrolledCount)
                .teacherUserId(teacher.getId())
                .teacherProfileId(tp.getId())
                .teacherName(teacher.getName())
                .teacherProfileImageUrl(teacher.getProfileImageUrl())

                .teacherNaegongScore(tp.getNaegongScore())
                .build();
    }
}
