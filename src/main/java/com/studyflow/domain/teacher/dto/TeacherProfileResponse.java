package com.studyflow.domain.teacher.dto;

import com.studyflow.domain.subject.dto.response.SubjectResponse;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Getter
public class TeacherProfileResponse {

    private final Long id;
    private final String career;          // 대학교
    private final String major;           // 전공
    private final String admissionYear;   // 학번
    private final String awards;
    private final String address;
    private final String teachingStyle;
    private final String introduction;
    private final Integer naegongScore;
    private final BigDecimal totalTeachingHours;
    private final List<SubjectResponse> specialtySubjects;   // 전문 과목

    public TeacherProfileResponse(TeacherProfile profile) {
        this.id                  = profile.getId();
        this.career              = profile.getCareer();
        this.major               = profile.getMajor();
        this.admissionYear       = profile.getAdmissionYear();
        this.awards              = profile.getAwards();
        this.address             = profile.getAddress();
        this.teachingStyle       = profile.getTeachingStyle();
        this.introduction        = profile.getIntroduction();
        this.naegongScore        = profile.getNaegongScore();
        this.totalTeachingHours  = profile.getTotalTeachingHours();
        this.specialtySubjects   = profile.getSpecialtySubjects().stream()
                .sorted(Comparator.comparing(Subject::getId))
                .map(SubjectResponse::from)
                .toList();
    }
}
