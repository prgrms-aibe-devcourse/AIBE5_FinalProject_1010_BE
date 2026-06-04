package com.studyflow.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 소셜 로그인 최초 가입 시 추가 정보 입력 요청 DTO.
 * POST /api/v1/auth/social-signup
 */
@Getter
@Setter
@NoArgsConstructor
public class SocialSignupRequest {

    /** Redis에서 임시 데이터를 조회하기 위한 UUID 토큰 */
    private String token;

    /** "STUDENT" | "TEACHER" */
    private String role;

    /** "MALE" | "FEMALE" */
    private String gender;

    /** "yyyy-MM-dd" */
    private String birthDate;

    /** 휴대폰 번호 (선택) */
    private String phone;

    private List<TermsAgreement> termsAgreements;

    @Getter
    @Setter
    @NoArgsConstructor
    public static class TermsAgreement {
        /** "SERVICE" | "PRIVACY" | "MARKETING" */
        private String termsType;

        @JsonProperty("isAgreed")
        private boolean isAgreed;
    }
}
