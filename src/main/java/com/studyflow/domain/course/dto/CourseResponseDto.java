package com.studyflow.domain.course.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CourseResponseDto {
    private Long id;
    private String title;
    private String description;
    private String instructorName;
    private Integer duration;
    private Double price;
}
