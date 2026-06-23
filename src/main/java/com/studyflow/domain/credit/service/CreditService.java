package com.studyflow.domain.credit.service;

import com.studyflow.domain.credit.entity.CreditAccount;
import com.studyflow.domain.credit.entity.CreditHistory;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.repository.CreditAccountRepository;
import com.studyflow.domain.credit.repository.CreditHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 크레딧 적립/차감 도메인 서비스.
 *
 * <p>충전(결제)·차감(AI질문/강의개설)을 한 곳에서 처리하고 변동 이력을 남긴다.
 * 차감은 호출 측(기능)의 트랜잭션에 합류해, "차감 성공했는데 기능 실패" 같은 불일치를 막는다.</p>
 */
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditHistoryRepository creditHistoryRepository;

    @Transactional(readOnly = true)
    public long getBalance(Long userId) {
        return creditAccountRepository.findByUserId(userId)
                .map(CreditAccount::getBalance)
                .orElse(0L);
    }

    @Transactional(readOnly = true)
    public Page<CreditHistory> getHistory(Long userId, Pageable pageable) {
        return creditHistoryRepository.findByUserIdOrderByIdDesc(userId, pageable);
    }

    /** 크레딧 적립(충전/환불). 적립 후 잔액 반환. */
    @Transactional
    public long charge(Long userId, long amount, CreditReason reason, Long refId) {
        CreditAccount account = getOrCreate(userId);
        long balance = account.charge(amount);
        creditHistoryRepository.save(CreditHistory.of(userId, amount, reason, refId, balance));
        return balance;
    }

    /** 크레딧 차감(기능 사용). 잔액 부족 시 InsufficientCreditException. 차감 후 잔액 반환. */
    @Transactional
    public long deduct(Long userId, long amount, CreditReason reason, Long refId) {
        CreditAccount account = getOrCreate(userId);
        long balance = account.deduct(amount);
        creditHistoryRepository.save(CreditHistory.of(userId, -amount, reason, refId, balance));
        return balance;
    }

    private CreditAccount getOrCreate(Long userId) {
        return creditAccountRepository.findByUserId(userId)
                .orElseGet(() -> creditAccountRepository.save(CreditAccount.createFor(userId)));
    }
}
