package com.studyflow.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "teacher_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String university;
    private String major;
    private String career;
    private String awards;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    private String gender;
    private Integer age;
    private String address;
    private String teachingMethod;

    private String certificationFileUrl;

    @Enumerated(EnumType.STRING)
    private TeacherVerifyStatus verifyStatus = TeacherVerifyStatus.PENDING;

    private int experienceScore = 0;

    @Builder
    public TeacherProfile(User user) {
        this.user = user;
    }

    public void updateProfile(String university, String major, String career,
                              String awards, String introduction, String gender,
                              Integer age, String address, String teachingMethod) {
        this.university = university;
        this.major = major;
        this.career = career;
        this.awards = awards;
        this.introduction = introduction;
        this.gender = gender;
        this.age = age;
        this.address = address;
        this.teachingMethod = teachingMethod;
    }

    public void uploadCertification(String fileUrl) {
        this.certificationFileUrl = fileUrl;
        this.verifyStatus = TeacherVerifyStatus.PENDING;
    }

    public void approve() {
        this.verifyStatus = TeacherVerifyStatus.APPROVED;
    }

    public void reject() {
        this.verifyStatus = TeacherVerifyStatus.REJECTED;
    }

    public void addExperienceScore(int score) {
        this.experienceScore += score;
    }
}
