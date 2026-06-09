package com.studyflow.domain.admin.dto;

import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminStudentDetailResponse(
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
        boolean isDeleted,
        LocalDateTime deletedAt,
        boolean marketingAgreed,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        // StudentProfile 정보
        Long profileId,
        String grade,
        String interestSubjects,
        String region,
        String goal,
        LocalDateTime profileCreatedAt,
        LocalDateTime profileUpdatedAt
) implements AdminUserDetailInterface {
    public static AdminStudentDetailResponse of(User user, StudentProfile profile) {
        return new AdminStudentDetailResponse(
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
                user.getIsDeleted() != 0,
                user.getDeletedAt(),
                user.isMarketingAgreed(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                profile != null ? profile.getId() : null,
                profile != null ? profile.getGrade() : null,
                profile != null ? profile.getInterestSubjects() : null,
                profile != null ? profile.getRegion() : null,
                profile != null ? profile.getGoal() : null,
                profile != null ? profile.getCreatedAt() : null,
                profile != null ? profile.getUpdatedAt() : null
        );
    }
}
