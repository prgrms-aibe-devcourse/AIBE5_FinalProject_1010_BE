package com.studyflow.domain.student.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "student_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StudentProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 20)
    private String grade;

    @Column(length = 500)
    private String interestSubjects;

    @Column(length = 100)
    private String region;

    @Column(columnDefinition = "TEXT")
    private String goal;

    // 팩토리 메서드: 회원가입 시 최소 정보로 StudentProfile 생성
    public static StudentProfile createForUser(User user) {
        StudentProfile p = new StudentProfile();
        p.user = user;
        return p;
    }

    // 프로필 수정 — 빈 문자열 포함 전달된 값 그대로 반영
    public void update(String goal, String grade, String interestSubjects, String region) {
        this.goal             = goal;
        this.grade            = grade;
        this.interestSubjects = interestSubjects;
        this.region           = region;
    }
}

