package com.studyflow.domain.admin.dto;

import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.UserRole;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminUserDetailResponse(
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
        LocalDateTime updatedAt
) {
    public static AdminUserDetailResponse from(User user) {
        return new AdminUserDetailResponse(
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
                user.getUpdatedAt()
        );
    }
}
