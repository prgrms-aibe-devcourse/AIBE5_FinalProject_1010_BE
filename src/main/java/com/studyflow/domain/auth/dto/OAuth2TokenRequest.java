package com.studyflow.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** POST /api/v1/auth/oauth2/token 요청 DTO */
@Getter
@Setter
@NoArgsConstructor
public class OAuth2TokenRequest {

    @NotBlank(message = "code는 필수입니다.")
    private String code;
}
