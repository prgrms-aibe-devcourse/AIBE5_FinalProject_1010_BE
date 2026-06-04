package com.studyflow.domain.auth.oauth2;

import com.studyflow.domain.user.enums.SocialProvider;

import java.util.Map;

/**
 * Kakao scope: profile_nickname, profile_image, account_email
 * - 제공: nickname(name 대체), profile_image, email
 * - 미제공: gender, birthDate, phone → null 반환
 */
public class KakaoOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public KakaoOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.KAKAO;
    }

    @Override
    public String getSocialId() {
        return String.valueOf(attributes.get("id"));
    }

    @Override
    public String getEmail() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;
        return (String) kakaoAccount.get("email");
    }

    @Override
    public String getName() {
        // 카카오는 실명을 제공하지 않으므로 nickname으로 대체합니다.
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) return null;
        return (String) profile.get("nickname");
    }

    @Override
    public String getProfileImageUrl() {
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        if (kakaoAccount == null) return null;
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
        if (profile == null) return null;
        return (String) profile.get("profile_image_url");
    }

    @Override
    public String getGender() {
        return null; // Kakao는 gender 미제공
    }

    @Override
    public String getBirthDate() {
        return null; // Kakao는 birthDate 미제공
    }

    @Override
    public String getPhone() {
        return null; // Kakao는 phone 미제공
    }
}
