package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.CourseResponseDto;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    public List<CourseResponseDto> getDummyCourses() {
        return List.of(
            CourseResponseDto.builder()
                .id(1L)
                .title("Java Programming Masterclass")
                .description("Learn Java from scratch to advanced concepts with practical examples.")
                .instructorName("John Doe")
                .duration(1200)
                .price(99000.0)
                .build(),
            CourseResponseDto.builder()
                .id(2L)
                .title("Spring Boot & JPA Deep Dive")
                .description("Build production-ready REST APIs with Spring Boot and Spring Data JPA.")
                .instructorName("Jane Smith")
                .duration(1800)
                .price(149000.0)
                .build(),
            CourseResponseDto.builder()
                .id(3L)
                .title("React & Next.js Modern Web Development")
                .description("Master modern frontend web development with React and Next.js framework.")
                .instructorName("Alex Kim")
                .duration(1500)
                .price(129000.0)
                .build()
        );
    }
}
