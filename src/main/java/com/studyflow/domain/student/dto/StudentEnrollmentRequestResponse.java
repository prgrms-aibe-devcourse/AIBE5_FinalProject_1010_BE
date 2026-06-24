package com.studyflow.domain.student.dto;

import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class StudentEnrollmentRequestResponse {

    private Long requestId;
    private Long courseId;
    private String courseTitle;
    private String teacherName;
    private EnrollmentRequestStatus status;
    private LocalDateTime createdAt;

    public static StudentEnrollmentRequestResponse from(EnrollmentRequest request) {
        return StudentEnrollmentRequestResponse.builder()
                .requestId(request.getId())
                .courseId(request.getCourse().getId())
                .courseTitle(request.getCourse().getTitle())
                .teacherName(request.getCourse().getTeacherProfile().getUser().getName())
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .build();
    }
}
