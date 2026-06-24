package com.studyflow.domain.auth.oauth2;

import com.studyflow.domain.user.enums.SocialProvider;

import java.util.Map;

/**
 * Google scope: openid, profile, email
 * - 제공: email, name, picture
 * - 미제공: gender, birthDate, phone → null 반환
 */
public class GoogleOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public GoogleOAuth2UserInfo(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.GOOGLE;
    }

    @Override
    public String getSocialId() {
        return (String) attributes.get("sub");
    }

    @Override
    public String getEmail() {
        return (String) attributes.get("email");
    }

    @Override
    public String getName() {
        return (String) attributes.get("name");
    }

    @Override
    public String getProfileImageUrl() {
        return (String) attributes.get("picture");
    }

    @Override
    public String getGender() {
        return null; // Google은 gender 미제공
    }

    @Override
    public String getBirthDate() {
        return null; // Google은 birthDate 미제공
    }

    @Override
    public String getPhone() {
        return null; // Google은 phone 미제공
    }
}
