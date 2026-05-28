package com.studyflow.domain.user.entity;
import com.studyflow.domain.constant.SocialProvider;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.studyflow.domain.constant.Role;
import java.time.LocalDateTime;

@Entity
@Table(name = "users") // PostgreSQL 예약어 충돌 방지
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)

public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider socialProvider;

    @Column(length = 255)
    private String socialId;

    // MySQL의 TINYINT(1)은 Java의 boolean으로 선언하면
    // PostgreSQL(Boolean), MySQL(TINYINT) 양쪽 모두 완벽하게 호환됩니다.
    @Column(nullable = false)
    private boolean isVerified = false;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime deletedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // 삭제 시 PK값을 넣기 위한 컬럼 (고유키 제약조건 방어용)
    @Column(nullable = false)
    private Long isDeleted = 0L;
}