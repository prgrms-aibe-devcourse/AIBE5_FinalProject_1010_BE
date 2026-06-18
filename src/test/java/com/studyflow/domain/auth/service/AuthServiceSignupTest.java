package com.studyflow.domain.auth.service;

import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
import com.studyflow.global.redis.RedisPrefixProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * AuthService.signup()의 DataIntegrityViolationException → AccountAlreadyExistsException 변환 경로를
 * 서비스 레벨 중복 체크를 우회하는 mock 설정으로 격리 검증합니다.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceSignupTest {

    @InjectMocks
    private AuthService authService;

    @Mock private UserRepository userRepository;
    @Mock private StudentProfileRepository studentProfileRepository;
    @Mock private TeacherProfileRepository teacherProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;
    @Mock private JavaMailSender mailSender;
    @Mock private JwtTokenProvider jwtTokenProvider;

    private static final String TEST_EMAIL = "test@studyflow.com";
    private static final String VERIFIED_TOKEN = "test-verified-token";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // verifiedToken → 이메일 반환 (토큰 유효 상태)
        when(valueOperations.get(RedisPrefixProvider.emailVerifiedTokenKey(VERIFIED_TOKEN)))
                .thenReturn(TEST_EMAIL);
        // 서비스 레벨 중복 체크 통과 — 이미 가입된 유저 없음으로 설정
        when(userRepository.findActiveByEmailAndSocialProvider(TEST_EMAIL, SocialProvider.LOCAL))
                .thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
    }

    @Test
    @DisplayName("서비스 레벨 중복 체크 통과 후 DB 유니크 제약 위반 시 AccountAlreadyExistsException(409)으로 변환")
    void signup_dataIntegrityViolation_throwsAccountAlreadyExistsException() {
        // saveAndFlush 시점에 DB 유니크 제약 위반 발생을 재현
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("unique constraint violation"));

        assertThatThrownBy(() -> authService.signup(buildSignupRequest()))
                .isInstanceOf(AccountAlreadyExistsException.class);
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private SignupRequest buildSignupRequest() {
        return SignupRequest.builder()
                .email(TEST_EMAIL)
                .password("Password123!")
                .passwordConfirm("Password123!")
                .name("테스트유저")
                .phone("01012345678")
                .role("STUDENT")
                .gender("MALE")
                .birthDate("2000-01-01")
                .verifiedToken(VERIFIED_TOKEN)
                .termsAgreements(List.of(
                        new SignupRequest.TermsAgreement(SignupRequest.TermsType.SERVICE,   true),
                        new SignupRequest.TermsAgreement(SignupRequest.TermsType.PRIVACY,   true),
                        new SignupRequest.TermsAgreement(SignupRequest.TermsType.MARKETING, false)
                ))
                .build();
    }
}
