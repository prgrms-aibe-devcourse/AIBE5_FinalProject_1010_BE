package com.studyflow.domain.credit.dto;

import java.time.LocalDateTime;

public record TeacherEarningsItemDto(
        Long id,
        Long amount,
        String reason,
        Long balanceAfter,
        LocalDateTime createdAt,
        String courseTitle
) {}
