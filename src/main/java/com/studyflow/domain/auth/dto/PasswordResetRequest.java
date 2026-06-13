package com.studyflow.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordResetRequest {

    @NotBlank
    private String token;

    @NotBlank
    private String newPassword;

    @NotBlank
    private String newPasswordConfirm;
}
