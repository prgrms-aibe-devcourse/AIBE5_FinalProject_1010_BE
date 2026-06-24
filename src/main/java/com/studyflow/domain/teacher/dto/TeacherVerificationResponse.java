package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.DocumentType;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherVerificationResponse {

    private Long id;
    private DocumentType documentType;
    private String documentUrl;
    private VerificationStatus status;
    private String description;
    private String rejectedReason;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static TeacherVerificationResponse from(TeacherVerification v) {
        return TeacherVerificationResponse.builder()
                .id(v.getId())
                .documentType(v.getDocumentType())
                .documentUrl(v.getDocumentUrl())
                .status(v.getStatus())
                .description(v.getDescription())
                .rejectedReason(v.getRejectedReason())
                .reviewedAt(v.getReviewedAt())
                .createdAt(v.getCreatedAt())
                .updatedAt(v.getUpdatedAt())
                .build();
    }
}
