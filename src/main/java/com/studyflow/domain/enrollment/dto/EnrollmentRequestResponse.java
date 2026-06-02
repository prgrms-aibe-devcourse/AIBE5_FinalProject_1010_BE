package com.studyflow.domain.enrollment.dto;

import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 수강 신청 응답 — chatRoomId를 포함해 프론트가 신청 직후 채팅창으로 이동할 수 있다
@Getter
@Builder
public class EnrollmentRequestResponse {

    private Long id;
    private Long courseId;
    private EnrollmentRequestStatus status;  // 신청 직후 항상 PENDING
    private Long chatRoomId;                 // 자동 개설된 선생님-학생 채팅방 ID
    private LocalDateTime createdAt;

    public static EnrollmentRequestResponse of(EnrollmentRequest request, Long chatRoomId) {
        return EnrollmentRequestResponse.builder()
                .id(request.getId())
                .courseId(request.getCourse().getId())
                .status(request.getStatus())
                .chatRoomId(chatRoomId)
                .createdAt(request.getCreatedAt())
                .build();
    }
}
