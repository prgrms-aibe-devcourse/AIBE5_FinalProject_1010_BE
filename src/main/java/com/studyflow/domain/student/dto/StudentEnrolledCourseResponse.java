package com.studyflow.domain.student.dto;

import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StudentEnrolledCourseResponse {

    private Long courseId;
    private String title;
    private String subjectName;
    private TargetGrade targetGrade;
    private int pricePerSession;
    private int maxStudents;
    private int durationMinutes;
    private String thumbnailUrl;
    private CourseStatus courseStatus;
    private EnrollmentStatus enrollmentStatus;

    public static StudentEnrolledCourseResponse from(Enrollment enrollment) {
        return StudentEnrolledCourseResponse.builder()
                .courseId(enrollment.getCourse().getId())
                .title(enrollment.getCourse().getTitle())
                .subjectName(enrollment.getCourse().getSubject().getName())
                .targetGrade(enrollment.getCourse().getTargetGrade())
                .pricePerSession(enrollment.getCourse().getPricePerSession())
                .maxStudents(enrollment.getCourse().getMaxStudents())
                .durationMinutes(enrollment.getCourse().getDurationMinutes())
                .thumbnailUrl(enrollment.getCourse().getThumbnailUrl())
                .courseStatus(enrollment.getCourse().getStatus())
                .enrollmentStatus(enrollment.getStatus())
                .build();
    }
}
