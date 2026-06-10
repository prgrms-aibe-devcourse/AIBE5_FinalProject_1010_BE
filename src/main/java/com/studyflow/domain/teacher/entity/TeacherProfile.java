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

    // 내공 점수 적립/차감 (예: QnA 답변 채택). 변동 후 누적 점수를 반환한다.
    public int addNaegongScore(int delta) {
        int updated = (this.naegongScore == null ? 0 : this.naegongScore) + delta;
        this.naegongScore = Math.max(0, updated);
        return this.naegongScore;
    }

    // 프로필 수정 — 빈 문자열 포함 전달된 값 그대로 반영
    public void update(String address, String introduction, String teachingStyle) {
        this.address       = address;
        this.introduction  = introduction;
        this.teachingStyle = teachingStyle;
    }

    // 관리자 승인을 통한 인증 정보 반영
    public void updateVerifiedInfo(String awards, String career, String education) {
        this.awards    = awards;
        this.career    = career;
        this.education = education;
    }

}

