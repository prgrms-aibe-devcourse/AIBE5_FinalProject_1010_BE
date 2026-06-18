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
    private final boolean isListed;       // 선생님 찾기 목록 노출 여부 (마이페이지 토글)
    // 수업 찾기에 노출 중인(공개+모집중) 수업 보유 여부 — true면 노출 토글을 끌 수 없음
    private final boolean hasListedCourses;
    private final List<SubjectResponse> specialtySubjects;   // 전문 과목

    public TeacherProfileResponse(TeacherProfile profile) {
        this(profile, false);
    }

    public TeacherProfileResponse(TeacherProfile profile, boolean hasListedCourses) {
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
        this.isListed            = profile.isListed();
        this.hasListedCourses    = hasListedCourses;
        this.specialtySubjects   = profile.getSpecialtySubjects().stream()
                .sorted(Comparator.comparing(Subject::getId))
                .map(SubjectResponse::from)
                .toList();
    }
}
