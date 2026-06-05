package com.studyflow.domain.student.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class StudentProfileUpdateRequest {

    private String goal;
    private String grade;
    private String interestSubjects;
    private String region;
}
