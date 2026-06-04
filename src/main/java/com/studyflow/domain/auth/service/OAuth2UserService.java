package com.studyflow.domain.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.auth.dto.PendingSocialUserData;
import com.studyflow.domain.auth.oauth2.*;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.redis.RedisPrefixProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 소셜 로그인 성공 후 유저 정보를 처리하는 서비스.
 * Spring Security가 토큰 교환 후 자동으로 호출합니다.
 *
 * <p>기존 회원: userId/role을 attributes에 담아 OAuth2SuccessHandler에서 JWT 발급
 * <p>신규 회원: 소셜 정보를 Redis에 임시 저장(10분)하고 pendingSocialToken을 attributes에 담아
 *             OAuth2SuccessHandler에서 추가 정보 입력 폼으로 리다이렉트
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OAuth2UserService implements org.springframework.security.oauth2.client.userinfo.OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    /** 추가 정보 입력 대기 Redis TTL (10분) */
    private static final long PENDING_TTL_MINUTES = 10L;

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 구현체로 소셜에서 유저 정보 가져오기
        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);

        // 2. 소셜 제공자 구분 (google / kakao / naver)
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        OAuth2UserInfo userInfo = resolveUserInfo(registrationId, oAuth2User.getAttributes());

        // 3. DB에서 기존 회원 조회
        Map<String, Object> attributes = new HashMap<>(oAuth2User.getAttributes());
        String nameAttributeKey = resolveNameAttributeKey(registrationId);

        return userRepository.findActiveByEmailAndSocialProvider(userInfo.getEmail(), userInfo.getProvider())
                .map(existingUser -> buildExistingUserResult(existingUser, attributes, nameAttributeKey))
                .orElseGet(() -> buildPendingUserResult(userInfo, attributes, nameAttributeKey));
    }

    /** 기존 회원: userId + role을 attributes에 추가해 JWT 발급 흐름으로 진행 */
    private DefaultOAuth2User buildExistingUserResult(User user,
                                                       Map<String, Object> attributes,
                                                       String nameAttributeKey) {
        attributes.put("userId", user.getId());
        attributes.put("role", user.getRole().name());
        return new DefaultOAuth2User(
                Collections.singleton(() -> "ROLE_" + user.getRole().name()),
                attributes,
                nameAttributeKey
        );
    }

    /**
     * 신규 회원: 소셜에서 받은 데이터를 Redis에 임시 저장하고,
     * pendingSocialToken을 attributes에 담아 추가 정보 입력 흐름으로 진행.
     */
    private DefaultOAuth2User buildPendingUserResult(OAuth2UserInfo userInfo,
                                                      Map<String, Object> attributes,
                                                      String nameAttributeKey) {
        String tempToken = UUID.randomUUID().toString();

        PendingSocialUserData pendingData = new PendingSocialUserData(
                userInfo.getEmail(),
                userInfo.getName(),
                userInfo.getProfileImageUrl(),
                userInfo.getSocialId(),
                userInfo.getProvider().name(),
                userInfo.getGender(),
                userInfo.getBirthDate(),
                userInfo.getPhone()
        );

        try {
            String json = objectMapper.writeValueAsString(pendingData);
            redisTemplate.opsForValue().set(
                    RedisPrefixProvider.socialPendingKey(tempToken),
                    json,
                    PENDING_TTL_MINUTES,
                    TimeUnit.MINUTES
            );
        } catch (JsonProcessingException e) {
            log.error("소셜 임시 데이터 Redis 저장 실패: {}", e.getMessage());
            throw new OAuth2AuthenticationException("소셜 로그인 처리 중 오류가 발생했습니다.");
        }

        log.info("신규 소셜 유저 감지 — provider={}, tempToken={}", userInfo.getProvider(), tempToken);

        attributes.put("pendingSocialToken", tempToken);
        return new DefaultOAuth2User(
                Collections.emptyList(), // 아직 권한 없음
                attributes,
                nameAttributeKey
        );
    }

    /** 소셜 제공자별 OAuth2UserInfo 구현체 선택 */
    private OAuth2UserInfo resolveUserInfo(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> new GoogleOAuth2UserInfo(attributes);
            case "kakao"  -> new KakaoOAuth2UserInfo(attributes);
            case "naver"  -> new NaverOAuth2UserInfo(attributes);
            default -> throw new OAuth2AuthenticationException("지원하지 않는 소셜 로그인: " + registrationId);
        };
    }

    /** 소셜 제공자별 name attribute key 반환 (DefaultOAuth2User 생성에 필요) */
    private String resolveNameAttributeKey(String registrationId) {
        return switch (registrationId.toLowerCase()) {
            case "google" -> "sub";
            case "kakao"  -> "id";
            case "naver"  -> "response";
            default -> "id";
        };
    }
}
