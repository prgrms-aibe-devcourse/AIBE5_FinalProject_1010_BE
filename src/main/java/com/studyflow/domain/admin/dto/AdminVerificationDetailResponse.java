package com.studyflow.domain.admin.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.DocumentType;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AdminVerificationDetailResponse {

    // 목록 항목
    private Long verificationId;
    private Long userId;
    private String teacherName;
    private VerificationStatus status;
    private LocalDateTime createdAt;

    // 상세 추가 항목
    private String description;
    private String awards;
    private String career;
    private String education;
    private DocumentType documentType;
    private String documentUrl;
    private String rejectedReason;
    private LocalDateTime reviewedAt;

    public static AdminVerificationDetailResponse from(TeacherVerification v, TeacherProfile profile) {
        return AdminVerificationDetailResponse.builder()
                .verificationId(v.getId())
                .userId(v.getUser().getId())
                .teacherName(v.getUser().getName())
                .status(v.getStatus())
                .createdAt(v.getCreatedAt())
                .description(v.getDescription())
                .awards(v.getAwards() != null ? v.getAwards() : (profile != null ? profile.getAwards() : null))
                .career(v.getCareer() != null ? v.getCareer() : (profile != null ? profile.getCareer() : null))
                .education(v.getEducation() != null ? v.getEducation() : (profile != null ? profile.getEducation() : null))
                .documentType(v.getDocumentType())
                .documentUrl(v.getDocumentUrl())
                .rejectedReason(v.getRejectedReason())
                .reviewedAt(v.getReviewedAt())
                .build();
    }
}
