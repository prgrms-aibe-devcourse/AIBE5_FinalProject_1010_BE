package com.studyflow.domain.course.dto.create;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

// 수업 등록 응답 DTO — 생성된 수업의 핵심 정보만 반환
// 상세 정보가 필요하면 GET /api/v1/courses/{id} 호출
@Getter
@Builder
public class CourseCreateResponse {

    private Long id;                // 생성된 수업 ID
    private String title;           // 수업명
    private CourseStatus status;    // 생성 직후 항상 RECRUITING
    private LocalDateTime createdAt;

    public static CourseCreateResponse from(Course course) {
        return CourseCreateResponse.builder()
                .id(course.getId())
                .title(course.getTitle())
                .status(course.getStatus())
                .createdAt(course.getCreatedAt())
                .build();
    }
}
