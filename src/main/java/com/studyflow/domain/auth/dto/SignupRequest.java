package com.studyflow.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 회원 가입 요청 DTO
 * JSON 예시:
 * {
 *   "email":"student@example.com",
 *   "password":"Password123!",
 *   "name":"이학생",
 *   "phone":"01012345678",
 *   "role":"STUDENT",
 *   "termsAgreements": [ {"termsType":"SERVICE","isAgreed":true}, ... ]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

	@Email
	@NotBlank
	private String email;

	@NotBlank
	@Size(min = 8, max = 128)
	private String password;

	@NotBlank
	private String passwordConfirm;

	@NotBlank
	private String name;

	@Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 '-' 없이 10자리 또는 11자리 숫자여야 합니다.")
	private String phone;

	/** role은 문자열로 전달받아 서비스 레이어에서 변환 처리합니다 (예: STUDENT, TEACHER, ADMIN) */
	@NotBlank
	private String role;

	@NotBlank
	@Pattern(regexp = "MALE|FEMALE", flags = Pattern.Flag.CASE_INSENSITIVE)
	private String gender;

	@NotBlank
	@Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "birthDate must be in format yyyy-MM-dd")
	private String birthDate;

	@NotEmpty
	private List<TermsAgreement> termsAgreements;

	@Getter
	@Setter
	@NoArgsConstructor
	@AllArgsConstructor
	@Builder
	public static class TermsAgreement {
		private TermsType termsType;
		@JsonProperty("isAgreed")
		private boolean isAgreed;
	}

	public enum TermsType {
		SERVICE,
		PRIVACY,
		MARKETING
	}
}
