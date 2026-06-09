package com.studyflow.domain.admin.dto;

public record UserCountByRoleResponse(
        long total,
        long student,
        long teacher,
        long admin
) {
}
