package com.studyflow.domain.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RejectVerificationRequest {

    @Size(max = 1000, message = "거절 사유는 1000자 이내로 입력해주세요.")
    private String rejectReason;
}
