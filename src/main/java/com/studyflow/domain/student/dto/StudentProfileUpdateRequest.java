package com.studyflow.domain.student.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@NoArgsConstructor
public class StudentProfileUpdateRequest {

    @Size(max = 5000)
    private String goal;

    @Size(max = 20)
    private String grade;

    @Size(max = 500)
    private String interestSubjects;

    @Size(max = 100)
    private String region;
}
