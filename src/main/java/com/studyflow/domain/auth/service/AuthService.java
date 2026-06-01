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
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final PasswordEncoder passwordEncoder;

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
            birthDateParsed = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "생년월일 형식이 올바르지 않습니다: " + request.getBirthDate());
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
        // TODO: Redis 도입 시 refresh token을 Redis에 저장
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

        return new LoginResponse(accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }
    
    public ReissueResponse reissue(Long userId, String refreshToken) {
        // TODO: Redis 도입 시 refreshToken 블랙리스트 처리 필요
        // refreshToken 파라미터가 현재 사용되지 않음
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // role 정보 추출 (1계정 2권한 허용 시 수정 필요)
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("권한 정보가 존재하지 않습니다."));
        // 'ROLE_' 접두사가 붙어있는 경우 접두사를 제거하여 순수한 role 문자열(STUDENT 등)을 사용
        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }
        String newAccessToken = jwtTokenProvider.createAccessToken(userId, role);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId, role);
        return new ReissueResponse(newAccessToken, newRefreshToken,
                jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }

    public void logout(String refreshToken) {
        // TODO: Redis에서 refreshToken을 삭제
    }
}
