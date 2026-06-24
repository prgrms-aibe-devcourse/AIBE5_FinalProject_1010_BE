package com.studyflow.domain.auth.oauth2;

import com.studyflow.domain.user.enums.SocialProvider;

import java.util.Map;

/**
 * Naver scope: name, email, nickname, profile_image, gender, birthday, birthyear, mobile
 * - 제공: email, name, profile_image, gender, birthDate(birthyear+birthday 조합), phone
 * - gender 응답값: "M" → "MALE", "F" → "FEMALE", "U" → null
 * - birthday 응답값: "MM-DD" 형식 → birthyear와 조합하여 "yyyy-MM-dd" 생성
 */
public class NaverOAuth2UserInfo implements OAuth2UserInfo {

    private final Map<String, Object> attributes;

    public NaverOAuth2UserInfo(Map<String, Object> attributes) {
        // 네이버는 실제 유저 정보가 "response" 키 하위에 중첩되어 있습니다.
        this.attributes = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public SocialProvider getProvider() {
        return SocialProvider.NAVER;
    }

    @Override
    public String getSocialId() {
        return (String) attributes.get("id");
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
        return (String) attributes.get("profile_image");
    }

    @Override
    public String getGender() {
        String gender = (String) attributes.get("gender");
        if (gender == null) return null;
        return switch (gender) {
            case "M" -> "MALE";
            case "F" -> "FEMALE";
            default  -> null; // "U"(알 수 없음) 등
        };
    }

    @Override
    public String getBirthDate() {
        // birthyear: "yyyy", birthday: "MM-DD" → "yyyy-MM-dd"
        String birthyear = (String) attributes.get("birthyear");
        String birthday  = (String) attributes.get("birthday");
        if (birthyear == null || birthday == null) return null;
        return birthyear + "-" + birthday;
    }

    @Override
    public String getPhone() {
        // 네이버 mobile 형식: "010-1234-5678" → 하이픈 제거하여 저장
        String mobile = (String) attributes.get("mobile");
        if (mobile == null) return null;
        return mobile.replace("-", "");
    }
}
