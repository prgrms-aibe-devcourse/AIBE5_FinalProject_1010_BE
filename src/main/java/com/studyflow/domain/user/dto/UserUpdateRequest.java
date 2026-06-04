package com.studyflow.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    @NotBlank(message = "전화번호는 필수입니다.")
    @Pattern(regexp = "^\\d{10,11}$", message = "전화번호는 '-' 없이 10자리 또는 11자리 숫자여야 합니다.")
    private String phone;

    @NotBlank(message = "성별은 필수입니다.")
    @Pattern(regexp = "MALE|FEMALE", flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "성별은 MALE 또는 FEMALE 이어야 합니다.")
    private String gender;

    @NotBlank(message = "생년월일은 필수입니다.")
    @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "생년월일은 yyyy-MM-dd 형식이어야 합니다.")
    private String birthDate;

    @NotNull(message = "마케팅 수신 동의 여부는 필수입니다.")
    private Boolean marketingAgreed;

    /** 프로필 이미지 URL - null 허용 (삭제 시 null 전달) */
    private String profileImageUrl;
}
