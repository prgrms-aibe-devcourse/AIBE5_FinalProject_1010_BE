package com.studyflow.domain.teacher.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TeacherProfileUpdateRequest {

    @Size(max = 200)
    private String address;

    @Size(max = 5000)
    private String awards;

    @Size(max = 5000)
    private String career;

    @Size(max = 300)
    private String education;

    @Size(max = 5000)
    private String introduction;

    @Size(max = 500)
    private String teachingStyle;
}
