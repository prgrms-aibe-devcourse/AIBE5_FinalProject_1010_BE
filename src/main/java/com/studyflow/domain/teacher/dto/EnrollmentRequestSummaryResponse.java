package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.student.entity.StudentProfile;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class EnrollmentRequestSummaryResponse {

    private Long requestId;
    private Long courseId;
    private String courseTitle;
    private StudentInfo student;
    private String introduction;
    private String goal;
    private String preferredSchedule;
    private String preferredStart;
    private String message;
    private EnrollmentRequestStatus status;
    private LocalDateTime createdAt;

    @Getter
    @Builder
    public static class StudentInfo {
        private Long userId;
        private String name;
        private String grade;
        private String region;
        private String goal;
    }

    public static EnrollmentRequestSummaryResponse of(EnrollmentRequest request, StudentProfile studentProfile) {
        StudentInfo studentInfo = StudentInfo.builder()
                .userId(request.getUser().getId())
                .name(request.getUser().getName())
                .grade(studentProfile != null ? studentProfile.getGrade() : null)
                .region(studentProfile != null ? studentProfile.getRegion() : null)
                .goal(studentProfile != null ? studentProfile.getGoal() : null)
                .build();

        return EnrollmentRequestSummaryResponse.builder()
                .requestId(request.getId())
                .courseId(request.getCourse().getId())
                .courseTitle(request.getCourse().getTitle())
                .student(studentInfo)
                .introduction(request.getIntroduction())
                .goal(request.getGoal())
                .preferredSchedule(request.getPreferredScheduleNote())
                .preferredStart(request.getPreferredStart())
                .message(request.getMessage())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
