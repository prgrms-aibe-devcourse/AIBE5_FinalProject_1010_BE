package com.studyflow.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 소셜 로그인 후 추가 정보 입력 전까지 Redis에 임시 저장하는 사용자 데이터.
 * TTL: 10분 (OAuth2UserService 참고)
 * Jackson으로 JSON 직렬화/역직렬화됩니다.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PendingSocialUserData {

    private String email;
    private String name;
    private String profileImageUrl;
    private String socialId;

    /** SocialProvider enum 이름 (예: "GOOGLE", "KAKAO", "NAVER") */
    private String provider;

    /** 소셜에서 받아온 gender ("MALE"/"FEMALE") — 제공하지 않으면 null */
    private String gender;

    /** 소셜에서 받아온 birthDate ("yyyy-MM-dd") — 제공하지 않으면 null */
    private String birthDate;

    /** 소셜에서 받아온 phone — 제공하지 않으면 null */
    private String phone;
}
