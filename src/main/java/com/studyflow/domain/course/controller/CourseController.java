package com.studyflow.domain.course.controller;

import com.studyflow.domain.course.dto.CourseResponseDto;
import com.studyflow.domain.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    @GetMapping
    public List<CourseResponseDto> getCourses() {
        return courseService.getDummyCourses();
    }
}
