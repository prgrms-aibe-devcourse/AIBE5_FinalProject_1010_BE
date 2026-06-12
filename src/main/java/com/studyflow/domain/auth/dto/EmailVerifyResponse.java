package com.studyflow.domain.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class EmailVerifyResponse {
    private String verifiedToken;
}
