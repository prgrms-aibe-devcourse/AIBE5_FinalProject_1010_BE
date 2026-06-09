package com.studyflow.domain.admin.dto;

import com.studyflow.domain.admin.entity.UserCountStatistics;

import java.time.LocalDate;

public record UserCountStatisticsResponse(
        LocalDate date,
        long newStudentCount,
        long newTeacherCount,
        long newAdminCount,
        long deletedStudentCount,
        long deletedTeacherCount,
        long deletedAdminCount
) {
    public static UserCountStatisticsResponse from(UserCountStatistics statistics) {
        return new UserCountStatisticsResponse(
                statistics.getDate(),
                statistics.getNewStudentCount(),
                statistics.getNewTeacherCount(),
                statistics.getNewAdminCount(),
                statistics.getDeletedStudentCount(),
                statistics.getDeletedTeacherCount(),
                statistics.getDeletedAdminCount()
        );
    }
}
