package com.studyflow.domain.credit.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequestDto {
    private long amount;
    private String bankName;
    private String accountNumber;
    private String accountHolder;
}
