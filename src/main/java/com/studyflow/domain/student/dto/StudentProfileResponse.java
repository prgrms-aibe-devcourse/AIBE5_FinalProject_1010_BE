package com.studyflow.domain.student.dto;

import com.studyflow.domain.student.entity.StudentProfile;
import lombok.Getter;

@Getter
public class StudentProfileResponse {

    private final String goal;
    private final String grade;
    private final String interestSubjects;
    private final String region;

    public StudentProfileResponse(StudentProfile profile) {
        this.goal             = profile.getGoal();
        this.grade            = profile.getGrade();
        this.interestSubjects = profile.getInterestSubjects();
        this.region           = profile.getRegion();
    }
}
