package com.studyflow.domain.admin.dto;

import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;

import java.time.LocalDateTime;

public record AdminUserSummaryResponse(
        Long id,
        LocalDateTime createdAt,
        String name,
        UserRole role
) {
    public static AdminUserSummaryResponse from(User user) {
        return new AdminUserSummaryResponse(
                user.getId(),
                user.getCreatedAt(),
                user.getName(),
                user.getRole()
        );
    }
}
