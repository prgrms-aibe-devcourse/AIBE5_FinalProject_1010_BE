package com.studyflow.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 소셜 로그인 추가 정보 입력 폼에 사전 입력(pre-fill)할 데이터 응답 DTO.
 * Redis 임시 데이터에서 프론트엔드에 노출해도 안전한 필드만 포함합니다.
 * (email, socialId 등 민감한 식별자는 제외)
 */
@Getter
@AllArgsConstructor
public class SocialPendingInfoResponse {

    /** 소셜 제공자가 준 이름 (표시용) */
    private String name;

    /** 소셜 제공자가 준 프로필 이미지 URL (표시용) */
    private String profileImageUrl;

    /** "MALE" / "FEMALE" / null — 소셜 미제공 시 null */
    private String gender;

    /** "yyyy-MM-dd" / null — 소셜 미제공 시 null */
    private String birthDate;

    /** 휴대폰 번호 / null — 소셜 미제공 시 null */
    private String phone;
}
