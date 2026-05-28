package com.studyflow.domain.user.entity;
import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.management.relation.Role;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email_social_isdeleted", columnNames = {"email", "social_provider", "is_deleted"})
}) // PostgreSQL 예약어 충돌 방지
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "social_provider", nullable = false)
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

    // 삭제 시 PK값을 넣기 위한 컬럼 (고유키 제약조건 방어용)
    @Column(name = "is_deleted", nullable = false)
    private Long isDeleted = 0L;

    @Column(nullable = false)
    private boolean marketingAgreed = false;

    public static User createUser(SignupRequest request, PasswordEncoder passwordEncoder, boolean marketingAgreed) {
        UserRole userRole;
        if(request.getRole().equals("STUDENT")) {
            userRole = UserRole.STUDENT;
        } else if(request.getRole().equals("TEACHER")){
            userRole = UserRole.TEACHER;
        } else if(request.getRole().equals("ADMIN")) {
            userRole = UserRole.ADMIN;
        } else {
            throw new IllegalArgumentException("Invalid role: " + request.getRole());
        }
        return User.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .socialProvider(SocialProvider.LOCAL)
                .phone(request.getPhone())
                .role(userRole)
                .marketingAgreed(marketingAgreed)
                .isDeleted(0L)
                .isActive(true)
                .build();
    }
}