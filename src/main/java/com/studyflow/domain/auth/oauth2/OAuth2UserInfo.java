package com.studyflow.domain.auth.oauth2;

import com.studyflow.domain.user.enums.SocialProvider;

/**
 * Google / Kakao / Naver 각각의 응답 형식을 하나로 통일하는 인터페이스.
 * 각 구현체는 소셜별 응답 Map을 파싱하여 공통 필드를 제공합니다.
 *
 * - 제공 불가 필드(예: Google의 gender)는 null을 반환하며,
 *   OAuth2UserService에서 기본값으로 처리합니다.
 */
public interface OAuth2UserInfo {

    SocialProvider getProvider();

    /** 소셜 플랫폼의 고유 사용자 ID */
    String getSocialId();

    String getEmail();

    String getName();

    String getProfileImageUrl();

    /** 성별: "MALE" / "FEMALE" / null (제공 불가 시) */
    String getGender();

    /** 생년월일: "yyyy-MM-dd" 형식 / null (제공 불가 시) */
    String getBirthDate();

    /** 휴대전화번호 / null (제공 불가 시) */
    String getPhone();
}
