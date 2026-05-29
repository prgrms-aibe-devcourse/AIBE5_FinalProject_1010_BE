package com.studyflow.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// LoginResponse와 내용은 동일하지만 요구사항 변동에 대비해 별도의 DTO로 분리
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class ReissueResponse {
    private String accessToken;
    private String refreshToken;
    private long accessExpiresIn; // ms
    private long refreshExpiresIn; // ms
}
