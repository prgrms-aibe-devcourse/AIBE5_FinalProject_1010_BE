package com.studyflow.domain.admin.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.UserRole;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminTeacherDetailResponse(
        // User 정보
        Long id,
        String email,
        String name,
        String phone,
        String profileImageUrl,
        UserRole role,
        Gender gender,
        LocalDate birthDate,
        boolean isVerified,
        boolean isActive,
        Long isDeleted,
        LocalDateTime deletedAt,
        boolean marketingAgreed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // TeacherProfile 정보
        Long profileId,
        String education,
        String career,
        String awards,
        String address,
        String teachingStyle,
        String introduction,
        Integer naegongScore,
        BigDecimal totalTeachingHours,
        LocalDateTime profileCreatedAt,
        LocalDateTime profileUpdatedAt
) {
    public static AdminTeacherDetailResponse of(User user, TeacherProfile profile) {
        return new AdminTeacherDetailResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getProfileImageUrl(),
                user.getRole(),
                user.getGender(),
                user.getBirthDate(),
                user.isVerified(),
                user.isActive(),
                user.getIsDeleted(),
                user.getDeletedAt(),
                user.isMarketingAgreed(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                profile != null ? profile.getId() : null,
                profile != null ? profile.getEducation() : null,
                profile != null ? profile.getCareer() : null,
                profile != null ? profile.getAwards() : null,
                profile != null ? profile.getAddress() : null,
                profile != null ? profile.getTeachingStyle() : null,
                profile != null ? profile.getIntroduction() : null,
                profile != null ? profile.getNaegongScore() : null,
                profile != null ? profile.getTotalTeachingHours() : null,
                profile != null ? profile.getCreatedAt() : null,
                profile != null ? profile.getUpdatedAt() : null
        );
    }
}
