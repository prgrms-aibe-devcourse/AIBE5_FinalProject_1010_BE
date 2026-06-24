package com.studyflow.domain.credit.service;

import com.studyflow.domain.credit.dto.WithdrawRequestDto;
import com.studyflow.domain.credit.entity.WithdrawalRequest;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.enums.WithdrawalStatus;
import com.studyflow.domain.credit.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final CreditService creditService;

    // 사용자: 마일리지 환급 신청
    @Transactional
    public void requestWithdrawal(Long userId, WithdrawRequestDto dto) {
        if (dto.getAmount() <= 0) {
            throw new IllegalArgumentException("환급 요청 금액은 0보다 커야 합니다.");
        }
        
        // 1. 선 차감 시도 (잔액 부족 시 에러 발생)
        creditService.deduct(userId, dto.getAmount(), CreditReason.WITHDRAWAL, null);

        // 2. 환급 요청 내역 저장
        WithdrawalRequest request = WithdrawalRequest.create(
                userId, dto.getAmount(), dto.getBankName(), dto.getAccountNumber(), dto.getAccountHolder()
        );
        withdrawalRequestRepository.save(request);
    }

    // 관리자: 환급 요청 승인
    @Transactional
    public void approveWithdrawal(Long id) {
        WithdrawalRequest req = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환급 요청입니다."));
        
        if (req.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("대기 중인 환급 요청만 승인할 수 있습니다.");
        }

        req.approve();
    }

    // 관리자: 환급 요청 거절 (마일리지 원상 복구)
    @Transactional
    public void rejectWithdrawal(Long id) {
        WithdrawalRequest req = withdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 환급 요청입니다."));
        
        if (req.getStatus() != WithdrawalStatus.PENDING) {
            throw new IllegalStateException("대기 중인 환급 요청만 거절할 수 있습니다.");
        }

        req.reject();

        // 3. 거절 시 차감된 마일리지 복구 처리
        creditService.charge(req.getUserId(), req.getAmount(), CreditReason.REFUND, req.getId());
    }
}
