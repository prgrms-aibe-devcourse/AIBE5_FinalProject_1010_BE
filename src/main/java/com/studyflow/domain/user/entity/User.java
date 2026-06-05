package com.studyflow.domain.user.entity;
import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.audit.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_email_social_isdeleted", columnNames = {"email", "social_provider", "is_deleted"})
}) // PostgreSQL 예약어 충돌 방지
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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false)
    private boolean isActive = true;

    private LocalDateTime deletedAt;

    // 삭제 시 PK값을 넣기 위한 컬럼 (고유키 제약조건 방어용)
    @Column(name = "is_deleted", nullable = false)
    private Long isDeleted = 0L;

    @Column(nullable = false)
    private boolean marketingAgreed = false;

    public static User createUser(SignupRequest request, PasswordEncoder passwordEncoder, 
                                  boolean marketingAgreed, LocalDate birthDateParsed, 
                                  Gender genderEnum, UserRole userRole) {
        // builder 스타일 대신 필드 직접 할당 방식으로 User 인스턴스 생성
        User user = new User();
        user.email = request.getEmail();
        user.password = passwordEncoder.encode(request.getPassword());
        user.name = request.getName();
        user.socialProvider = SocialProvider.LOCAL;
        user.phone = request.getPhone();
        user.gender = genderEnum;
        user.birthDate = birthDateParsed;
        user.role = userRole;
        user.marketingAgreed = marketingAgreed;
        user.isDeleted = 0L;
        user.isActive = true;

        return user;
    }

    /**
     * 소셜 로그인 최초 가입용 팩토리 메서드.
     * 추가 정보 입력 폼에서 수집·변환된 강타입 값을 직접 받습니다.
     */
    public static User createSocialUser(String email, String name, String profileImageUrl,
                                        String socialId, SocialProvider provider,
                                        Gender gender, LocalDate birthDate, String phone,
                                        UserRole userRole, boolean marketingAgreed) {
        User user = new User();
        user.email = email;
        user.name = (name != null) ? name : "소셜유저";
        user.profileImageUrl = profileImageUrl;
        user.socialId = socialId;
        user.socialProvider = provider;
        user.role = userRole;
        user.phone = phone;
        user.gender = gender;
        user.birthDate = birthDate;
        user.isVerified = false;
        user.isActive = true;
        user.isDeleted = 0L;
        user.marketingAgreed = marketingAgreed;
        return user;
    }

    public void updateUser(String name, String phone, Gender gender,
                           LocalDate birthDate, Boolean marketingAgreed, String profileImageUrl) {
        this.name = name;
        this.phone = phone;
        this.gender = gender;
        this.birthDate = birthDate;
        this.marketingAgreed = marketingAgreed;
        this.profileImageUrl = profileImageUrl; // null 허용 (프로필 이미지 삭제 시 null)
    }

    public void changePassword(String encodedNewPassword) {
        this.password = encodedNewPassword;
    }

    public void deleteUser() {
        this.isDeleted = this.id;
        this.deletedAt = LocalDateTime.now();
        this.isActive = false;
    }
}

