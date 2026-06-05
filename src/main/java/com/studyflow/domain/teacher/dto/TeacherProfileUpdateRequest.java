package com.studyflow.domain.teacher.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TeacherProfileUpdateRequest {

    private String address;
    private String awards;
    private String career;
    private String education;
    private String introduction;
    private String teachingStyle;
}
