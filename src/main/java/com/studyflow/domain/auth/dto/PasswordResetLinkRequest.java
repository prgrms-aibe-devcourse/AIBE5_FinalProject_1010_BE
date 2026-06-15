package com.studyflow.domain.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class PasswordResetLinkRequest {

    @NotBlank
    @Email
    private String email;
}
