package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class TeacherProfileResponse {

    private final Long id;
    private final String education;
    private final String career;
    private final String awards;
    private final String address;
    private final String teachingStyle;
    private final String introduction;
    private final Integer naegongScore;
    private final BigDecimal totalTeachingHours;

    public TeacherProfileResponse(TeacherProfile profile) {
        this.id                  = profile.getId();
        this.education           = profile.getEducation();
        this.career              = profile.getCareer();
        this.awards              = profile.getAwards();
        this.address             = profile.getAddress();
        this.teachingStyle       = profile.getTeachingStyle();
        this.introduction        = profile.getIntroduction();
        this.naegongScore        = profile.getNaegongScore();
        this.totalTeachingHours  = profile.getTotalTeachingHours();
    }
}
