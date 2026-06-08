package com.studyflow.domain.user.dto;

import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminVerificationSummaryResponse {

    private Long verificationId;
    private Long userId;
    private String teacherName;
    private VerificationStatus status;
    private LocalDateTime createdAt;

    public static AdminVerificationSummaryResponse from(TeacherVerification v) {
        return AdminVerificationSummaryResponse.builder()
                .verificationId(v.getId())
                .userId(v.getUser().getId())
                .teacherName(v.getUser().getName())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .build();
    }
}
