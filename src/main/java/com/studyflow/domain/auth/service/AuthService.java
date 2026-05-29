package com.studyflow.domain.auth.service;

import com.studyflow.domain.auth.dto.LoginRequest;
import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.ReissueResponse;
import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.auth.exception.InvalidCredentialsException;
import com.studyflow.domain.auth.exception.TermsAgreementException;
import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.auth.exception.InvalidGenderException;
import com.studyflow.domain.auth.exception.InvalidRoleException;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
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
import com.studyflow.domain.auth.exception.InvalidBirthDateException;

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
            throw new TermsAgreementException("Service terms agreement must be accepted");
        }
        if (!privacyAgreed) {
            throw new TermsAgreementException("Privacy terms agreement must be accepted");
        }
        if (!marketingPresent) {
            throw new TermsAgreementException("Marketing terms agreement does not exist");
        }
        // SignupRequest의 birthDate를 LocalDate로 변환, 실패 시 커스텀 예외를 던집니다.
        LocalDate birthDateParsed;
        try {
            birthDateParsed = LocalDate.parse(request.getBirthDate(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidBirthDateException("Invalid birthDate: " + request.getBirthDate(), e);
        }

        // SignupRequest의 gender를 user.enum.Gender로 변환, 실패 시 커스텀 예외
        Gender genderEnum;
        try {
            genderEnum = Gender.valueOf(request.getGender().toUpperCase());
        } catch (Exception e) {
            throw new InvalidGenderException("Invalid gender: " + request.getGender());
        }

        // SignupRequest의 role을 user.enum.UserRole로 변환, 실패 시 커스텀 예외
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (Exception e) {
            throw new InvalidRoleException("Invalid role: " + request.getRole());
        }

        // 이메일 중복이 있는지 확인하고, 있으면 예외를 던집니다.
        userRepository.findActiveByEmailAndSocialProvider(request.getEmail(), SocialProvider.LOCAL).ifPresent(u -> {
            throw new AccountAlreadyExistsException("Email already in use: " + request.getEmail());
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
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Invalid email or password");
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
}
