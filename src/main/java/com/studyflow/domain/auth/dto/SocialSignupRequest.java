package com.studyflow.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank(message = "토큰은 필수입니다.")
    private String token;

    @NotBlank(message = "역할은 필수입니다.")
    @Pattern(regexp = "STUDENT|TEACHER", flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "역할은 STUDENT 또는 TEACHER여야 합니다.")
    private String role;

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "MALE|FEMALE", flags = Pattern.Flag.CASE_INSENSITIVE,
             message = "성별은 MALE 또는 FEMALE이어야 합니다.")
    private String gender;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일 형식은 yyyy-MM-dd여야 합니다.")
    private String birthDate;

    /** 휴대폰 번호 (선택) */
    private String phone;

    @NotEmpty(message = "약관 동의 정보가 필요합니다.")
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
