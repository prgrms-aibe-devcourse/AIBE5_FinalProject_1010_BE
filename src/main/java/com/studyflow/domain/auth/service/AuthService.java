package com.studyflow.domain.auth.service;

import com.studyflow.domain.auth.dto.LoginRequest;
import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.SignupRequest;
import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.auth.exception.InvalidCredentialsException;
import com.studyflow.domain.auth.exception.TermsAgreementException;
import com.studyflow.domain.constant.SocialProvider;
import com.studyflow.domain.constant.UserRole;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService {
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
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

        // 이메일 중복이 있는지 확인하고, 있으면 예외를 던집니다.
        userRepository.findActiveByEmailAndSocialProvider(request.getEmail(), SocialProvider.LOCAL).ifPresent(u -> {
            throw new AccountAlreadyExistsException("Email already in use: " + request.getEmail());
        });

        User user = User.createUser(request, passwordEncoder, marketingAgreed);
        userRepository.save(user);
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
}
