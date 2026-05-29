package com.studyflow.domain.enrollment.dto;

import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 수업별 페이지 → 수강생 목록 탭에서 각 수강생을 표시하는 응답 DTO
@Getter
@Builder
public class EnrolledStudentResponse {

    private Long userId;
    private String name;
    private String email;
    private String profileImageUrl;
    private LocalDateTime enrolledAt;

    public static EnrolledStudentResponse from(Enrollment enrollment) {
        User user = enrollment.getUser();
        return EnrolledStudentResponse.builder()
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .profileImageUrl(user.getProfileImageUrl())
                .enrolledAt(enrollment.getEnrolledAt())
                .build();
    }
}
