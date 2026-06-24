package com.studyflow.domain.auth.dto;

import com.studyflow.domain.user.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private Long userId;
    private String name;
    private UserRole role;
    private String accessToken;
    private String refreshToken;
    private long accessExpiresIn; // ms
    private long refreshExpiresIn; // ms
}
