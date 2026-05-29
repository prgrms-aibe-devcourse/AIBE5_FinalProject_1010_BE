package com.studyflow.domain.teacher.entity;

import com.studyflow.domain.user.entity.User;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "teacher_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(length = 300)
    private String education;

    @Column(columnDefinition = "TEXT")
    private String career;

    @Column(columnDefinition = "TEXT")
    private String awards;

    private Integer age;

    @Column(length = 200)
    private String address;

    @Column(length = 500)
    private String teachingStyle;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(nullable = false)
    private Integer naegongScore = 0;

    // DECIMAL(10,1) 매핑을 위해 BigDecimal 사용
    @Column(nullable = false, precision = 10, scale = 1)
    private BigDecimal totalTeachingHours = BigDecimal.ZERO;


    // 팩토리 메서드: 회원가입 시 최소 정보로 TeacherProfile 생성
    public static TeacherProfile createForUser(User user) {
        TeacherProfile p = new TeacherProfile();
        p.user = user;
        return p;
    }

}

