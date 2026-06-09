package com.studyflow.domain.auth.service;

import com.studyflow.domain.auth.dto.LoginRequest;
import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.ReissueResponse;
import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.auth.exception.*;
import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.auth.exception.SignupRequestException;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.global.redis.RedisPrefixProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.concurrent.TimeUnit;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate redisTemplate;

    public void signup(SignupRequest request) {
        boolean serviceAgreed = false;
        boolean privacyAgreed = false;
        boolean marketingPresent = false;
        boolean marketingAgreed = false;
        for (SignupRequest.TermsAgreement ta : request.getTermsAgreements()) {
            if (ta.getTermsType() == SignupRequest.TermsType.SERVICE && ta.isAgreed()) {
                serviceAgreed = true;
            }
            if (ta.getTermsType() == SignupRequest.TermsType.PRIVACY && ta.isAgreed()) {
                privacyAgreed = true;
            }
            if (ta.getTermsType() == SignupRequest.TermsType.MARKETING) {
                // MARKETING은 동의 여부(true/false) 상관없이 항목 자체가 존재해야 함
                marketingPresent = true;
                marketingAgreed = ta.isAgreed();
            }
        }
        if (!serviceAgreed) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "서비스 약관에 동의해야 합니다.");
        }
        if (!privacyAgreed) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "개인정보 수집 및 이용에 동의해야 합니다.");
        }
        if (!marketingPresent) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "마케팅 약관 항목이 존재하지 않습니다.");
        }
        // SignupRequest의 birthDate를 LocalDate로 변환, 실패 시 커스텀 예외를 던집니다.
        LocalDate birthDateParsed;
        try {
            birthDateParsed = LocalDate.parse(request.getBirthDate(),
                    DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(java.time.format.ResolverStyle.STRICT));
        } catch (DateTimeParseException e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 날짜입니다: " + request.getBirthDate());
        }

        // SignupRequest의 gender를 user.enum.Gender로 변환, 실패 시 커스텀 예외
        Gender genderEnum;
        try {
            genderEnum = Gender.valueOf(request.getGender().toUpperCase());
        } catch (Exception e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 성별입니다: " + request.getGender());
        }

        // SignupRequest의 role을 user.enum.UserRole로 변환, 실패 시 커스텀 예외
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 권한입니다: " + request.getRole());
        }

        // 관리자 회원가입 시도 차단
        if (userRole == UserRole.ADMIN) {
            throw new SignupWithAdminException(ErrorCode.ACCESS_DENIED, "회원가입에서 허용되지 않는 권한입니다.");
        }

        // 이메일 중복이 있는지 확인하고, 있으면 예외를 던집니다.
        userRepository.findActiveByEmailAndSocialProvider(request.getEmail(), SocialProvider.LOCAL).ifPresent(u -> {
            throw new AccountAlreadyExistsException(ErrorCode.EMAIL_CONFLICT, "이미 사용 중인 이메일입니다: " + request.getEmail());
        });

        User user = User.createUser(request, passwordEncoder, marketingAgreed, birthDateParsed, genderEnum, userRole);
        userRepository.save(user);

        if(userRole == UserRole.STUDENT) {
            StudentProfile sp = StudentProfile.createForUser(user);
            studentProfileRepository.save(sp);
        } else if(userRole == UserRole.TEACHER) {
            TeacherProfile tp = TeacherProfile.createForUser(user);
            teacherProfileRepository.save(tp);
        }
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findActiveByEmailAndSocialProvider(request.getEmail(),SocialProvider.LOCAL)
                .orElseThrow(() -> new InvalidCredentialsException(ErrorCode.AUTH_LOGIN_FAILED, "이메일 또는 비밀번호가 일치하지 않습니다."));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException(ErrorCode.AUTH_LOGIN_FAILED, "이메일 또는 비밀번호가 일치하지 않습니다.");
        }

        // 성공: access + refresh 토큰 발급
        String role;
        if(user.getRole() == UserRole.STUDENT) {
            role = "STUDENT";
        } else if(user.getRole() == UserRole.TEACHER) {
            role = "TEACHER";
        } else if(user.getRole() == UserRole.ADMIN) {
            role = "ADMIN";
        } else {
            throw new IllegalStateException("Unexpected role: " + user.getRole());
        }
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), role);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), role);

        // Redis에 refresh token 저장: key = rt:{userId}, value = refreshToken, TTL = refreshToken 만료 시간
        redisTemplate.opsForValue().set(
                RedisPrefixProvider.refreshTokenKey(user.getId()),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                java.util.concurrent.TimeUnit.MILLISECONDS
        );

        return new LoginResponse(user.getId(), user.getName(), user.getRole(),
                accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }
    
    public ReissueResponse reissue(String refreshToken, Long userId) {
        // Redis에서 저장된 refresh token 조회 후 유효성 검증
        // 추후 동시성 문제 고려 필요
        String storedRefreshToken = redisTemplate.opsForValue().get(RedisPrefixProvider.refreshTokenKey(userId));
        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            throw new RefreshTokenNotInRedisException(ErrorCode.AUTH_INVALID_TOKEN,
                    "서버의 refresh token 정보와 일치하지 않거나 존재하지 않습니다.");
        }

        // DB에서 최신 사용자 정보 조회 — role을 토큰에서 추출하지 않고 DB 기준으로 발급
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        String role = user.getRole().name();
        String newAccessToken  = jwtTokenProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, role);

        // 기존 refresh token을 새 refresh token으로 교체: key = rt:{userId}, TTL = refreshToken 만료 시간
        redisTemplate.opsForValue().set(
                RedisPrefixProvider.refreshTokenKey(userId),
                newRefreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        return new ReissueResponse(user.getId(), user.getName(), role,
                newAccessToken, newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }

    public void logout(Long userId) {
        // Redis에서 refresh token 삭제: key = rt:{userId}
        redisTemplate.delete(RedisPrefixProvider.refreshTokenKey(userId));
    }
}
