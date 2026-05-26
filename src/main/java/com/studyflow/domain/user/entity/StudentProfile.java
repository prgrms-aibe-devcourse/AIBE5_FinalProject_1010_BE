package com.studyflow.domain.user.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "student_profiles")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String grade;
    private String gender;
    private String region;
    private String subjects;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Builder
    public StudentProfile(User user) {
        this.user = user;
    }

    public void updateProfile(String grade, String gender, String region,
                              String subjects, String goal) {
        this.grade = grade;
        this.gender = gender;
        this.region = region;
        this.subjects = subjects;
        this.goal = goal;
    }
}
