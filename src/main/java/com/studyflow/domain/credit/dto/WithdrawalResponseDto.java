package com.studyflow.domain.credit.dto;

import com.studyflow.domain.credit.enums.WithdrawalStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponseDto {
    private Long id;
    private Long userId;
    private String email;
    private String name;
    private long amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
    private WithdrawalStatus status;
    private LocalDateTime createdAt;
}
