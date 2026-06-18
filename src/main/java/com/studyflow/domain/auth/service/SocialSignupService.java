package com.studyflow.domain.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.auth.dto.LoginResponse;
import com.studyflow.domain.auth.dto.PendingSocialUserData;
import com.studyflow.domain.auth.dto.SocialPendingInfoResponse;
import com.studyflow.domain.auth.dto.SocialSignupRequest;
import com.studyflow.domain.auth.entity.LoginHistory;
import com.studyflow.domain.auth.exception.AccountAlreadyExistsException;
import com.studyflow.domain.auth.exception.SignupRequestException;
import com.studyflow.domain.auth.repository.LoginHistoryRepository;
import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.auth.JwtTokenProvider;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.global.redis.RedisPrefixProvider;
import com.studyflow.global.util.UserAgentParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 소셜 로그인 최초 가입 시 추가 정보를 받아 회원 가입을 완료하는 서비스.
 * Redis에 임시 저장된 소셜 정보 + 폼 입력값을 합쳐 User와 프로필을 생성합니다.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class SocialSignupService {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginHistoryRepository loginHistoryRepository;

    /**
     * Redis 임시 데이터에서 폼 pre-fill용 정보를 조회합니다.
     * 소셜 제공자가 값을 주지 않은 필드는 null로 반환하며, FE에서 빈 입력칸으로 표시합니다.
     */
    @Transactional(readOnly = true)
    public SocialPendingInfoResponse getPendingInfo(String token) {
        String key = RedisPrefixProvider.socialPendingKey(token);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR,
                    "소셜 로그인 임시 세션이 만료되었습니다(10분). 다시 소셜 로그인을 시도해 주세요.");
        }
        try {
            PendingSocialUserData data = objectMapper.readValue(json, PendingSocialUserData.class);
            return new SocialPendingInfoResponse(
                    data.getName(),
                    data.getProfileImageUrl(),
                    data.getGender(),
                    data.getBirthDate(),
                    data.getPhone()
            );
        } catch (Exception e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "임시 데이터 처리 중 오류가 발생했습니다.");
        }
    }

    public LoginResponse completeSocialSignup(SocialSignupRequest request, String ipAddress, String userAgent) {

        // 1. 필수 필드 null 검증 — Redis 조회 전에 먼저 수행
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "토큰이 누락되었습니다.");
        }
        if (request.getGender() == null || request.getGender().isBlank()) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "성별은 필수입니다.");
        }
        if (request.getBirthDate() == null || request.getBirthDate().isBlank()) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "생년월일은 필수입니다.");
        }
        if (request.getRole() == null || request.getRole().isBlank()) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "역할은 필수입니다.");
        }

        // 2. Redis에서 임시 소셜 데이터 조회
        String key = RedisPrefixProvider.socialPendingKey(request.getToken());
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR,
                    "소셜 로그인 임시 세션이 만료되었습니다(10분). 다시 소셜 로그인을 시도해 주세요.");
        }

        PendingSocialUserData pendingData;
        try {
            pendingData = objectMapper.readValue(json, PendingSocialUserData.class);
        } catch (Exception e) {
            log.error("소셜 임시 데이터 파싱 오류: {}", e.getMessage());
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "임시 데이터 처리 중 오류가 발생했습니다.");
        }

        // 3. 약관 동의 검증
        validateTerms(request.getTermsAgreements());

        // 4. gender 변환 및 검증
        Gender gender;
        try {
            gender = Gender.valueOf(request.getGender().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 성별입니다: " + request.getGender());
        }

        // 5. birthDate 파싱 및 검증
        LocalDate birthDate;
        try {
            birthDate = LocalDate.parse(request.getBirthDate());
        } catch (Exception e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR,
                    "생년월일 형식이 올바르지 않습니다(yyyy-MM-dd): " + request.getBirthDate());
        }
        if (birthDate.isAfter(LocalDate.now())) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "생년월일은 오늘 이후일 수 없습니다.");
        }

        // 6. role 변환 및 검증 (ADMIN 불가)
        UserRole userRole;
        try {
            userRole = UserRole.valueOf(request.getRole().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "유효하지 않은 역할입니다: " + request.getRole());
        }
        if (userRole == UserRole.ADMIN) {
            throw new SignupRequestException(ErrorCode.ACCESS_DENIED, "회원가입에서 허용되지 않는 권한입니다.");
        }

        // 7. 중복 가입 방어 (동시 요청 등 race condition)
        SocialProvider provider = SocialProvider.valueOf(pendingData.getProvider());
        userRepository.findActiveByEmailAndSocialProvider(pendingData.getEmail(), provider).ifPresent(u -> {
            throw new AccountAlreadyExistsException(ErrorCode.EMAIL_CONFLICT, "이미 가입된 소셜 계정입니다.");
        });

        // 8. User 생성 (사용자가 입력한 gender/birthDate/role/phone 사용)
        boolean marketingAgreed = extractMarketingAgreed(request.getTermsAgreements());
        String phone = (request.getPhone() != null && !request.getPhone().isBlank())
                ? request.getPhone()
                : pendingData.getPhone(); // 폼 미입력 시 소셜 제공값 사용

        User user = User.createSocialUser(
                pendingData.getEmail(),
                pendingData.getName(),
                pendingData.getProfileImageUrl(),
                pendingData.getSocialId(),
                provider,
                gender,      // Gender enum 직접 전달
                birthDate,   // LocalDate 직접 전달
                phone,
                userRole,
                marketingAgreed
        );
        userRepository.save(user);

        // 9. 역할별 프로필 생성
        if (userRole == UserRole.STUDENT) {
            studentProfileRepository.save(StudentProfile.createForUser(user));
        } else {
            teacherProfileRepository.save(TeacherProfile.createForUser(user));
        }

        // 10. 임시 데이터 Redis에서 삭제
        redisTemplate.delete(key);
        log.info("소셜 신규 회원 가입 완료 — userId={}, provider={}", user.getId(), provider);

        // 11. JWT 발급 및 Redis에 refresh token 저장
        String accessToken  = jwtTokenProvider.createAccessToken(user.getId(), userRole.name());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), userRole.name());
        redisTemplate.opsForValue().set(
                RedisPrefixProvider.refreshTokenKey(user.getId()),
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpiration(),
                TimeUnit.MILLISECONDS
        );

        loginHistoryRepository.save(LoginHistory.of(
                user.getId(),
                ipAddress,
                UserAgentParser.extractDeviceInfo(userAgent),
                UserAgentParser.extractBrowser(userAgent)
        ));

        return new LoginResponse(user.getId(), user.getName(), user.getRole(),
                accessToken, refreshToken,
                jwtTokenProvider.getAccessTokenExpiration(), jwtTokenProvider.getRefreshTokenExpiration());
    }

    private void validateTerms(List<SocialSignupRequest.TermsAgreement> terms) {
        if (terms == null || terms.isEmpty()) {
            throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "약관 동의 정보가 필요합니다.");
        }
        boolean serviceAgreed = false;
        boolean privacyAgreed = false;
        boolean marketingPresent = false;

        for (SocialSignupRequest.TermsAgreement ta : terms) {
            if (ta == null || ta.getTermsType() == null) continue;
            if ("SERVICE".equalsIgnoreCase(ta.getTermsType()) && ta.isAgreed()) serviceAgreed = true;
            if ("PRIVACY".equalsIgnoreCase(ta.getTermsType()) && ta.isAgreed())  privacyAgreed = true;
            if ("MARKETING".equalsIgnoreCase(ta.getTermsType())) marketingPresent = true;
        }

        if (!serviceAgreed)   throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "서비스 약관에 동의해야 합니다.");
        if (!privacyAgreed)   throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "개인정보 수집 및 이용에 동의해야 합니다.");
        if (!marketingPresent) throw new SignupRequestException(ErrorCode.VALIDATION_ERROR, "마케팅 약관 항목이 필요합니다.");
    }

    private boolean extractMarketingAgreed(List<SocialSignupRequest.TermsAgreement> terms) {
        if (terms == null) return false;
        return terms.stream()
                .filter(ta -> "MARKETING".equalsIgnoreCase(ta.getTermsType()))
                .findFirst()
                .map(SocialSignupRequest.TermsAgreement::isAgreed)
                .orElse(false);
    }
}
