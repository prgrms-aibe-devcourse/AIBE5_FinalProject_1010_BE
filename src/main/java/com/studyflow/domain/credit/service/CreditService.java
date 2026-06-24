package com.studyflow.domain.credit.service;

import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.credit.dto.TeacherEarningsItemDto;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 마일리지 적립/차감 도메인 서비스.
 *
 * <p>충전(결제)·차감(AI질문/강의개설)을 한 곳에서 처리하고 변동 이력을 남긴다.
 * 차감은 호출 측(기능)의 트랜잭션에 합류해, "차감 성공했는데 기능 실패" 같은 불일치를 막는다.</p>
 */
@Service
@RequiredArgsConstructor
public class CreditService {

    private final CreditAccountRepository creditAccountRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final CourseRepository courseRepository;

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

    @Transactional(readOnly = true)
    public Page<TeacherEarningsItemDto> getEarningsHistory(Long userId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59, 999999999) : null;
        
        Page<CreditHistory> page = creditHistoryRepository.findEarningsWithDates(userId, CreditReason.ENROLLMENT_INCOME, startDateTime, endDateTime, pageable);
        
        List<Long> courseIds = page.getContent().stream()
                .filter(h -> h.getRefId() != null)
                .map(CreditHistory::getRefId)
                .distinct()
                .toList();
                
        Map<Long, String> courseTitles = courseIds.isEmpty() ? Map.of() : courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));
                
        return page.map(h -> new TeacherEarningsItemDto(
                h.getId(),
                h.getAmount(),
                h.getReason().name(),
                h.getBalanceAfter(),
                h.getCreatedAt(),
                h.getRefId() != null ? courseTitles.getOrDefault(h.getRefId(), "알 수 없는 수업") : "알 수 없는 수업"
        ));
    }

    @Transactional(readOnly = true)
    public Long getTotalEarnings(Long userId, LocalDate startDate, LocalDate endDate) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59, 999999999) : null;
        return creditHistoryRepository.sumEarningsWithDates(userId, CreditReason.ENROLLMENT_INCOME, startDateTime, endDateTime);
    }

    /** 마일리지 적립(충전/환불). 적립 후 잔액 반환. */
    @Transactional
    public long charge(Long userId, long amount, CreditReason reason, Long refId) {
        CreditAccount account = getOrCreateWithLock(userId);
        long balance = account.charge(amount);
        creditHistoryRepository.save(CreditHistory.of(userId, amount, reason, refId, balance));
        return balance;
    }

    /** 마일리지 차감(기능 사용). 잔액 부족 시 InsufficientCreditException. 차감 후 잔액 반환. */
    @Transactional
    public long deduct(Long userId, long amount, CreditReason reason, Long refId) {
        CreditAccount account = getOrCreateWithLock(userId);
        long balance = account.deduct(amount);
        creditHistoryRepository.save(CreditHistory.of(userId, -amount, reason, refId, balance));
        return balance;
    }

    private CreditAccount getOrCreate(Long userId) {
        return creditAccountRepository.findByUserId(userId)
                .orElseGet(() -> creditAccountRepository.save(CreditAccount.createFor(userId)));
    }

    private CreditAccount getOrCreateWithLock(Long userId) {
        return creditAccountRepository.findWithLockByUserId(userId)
                .orElseGet(() -> creditAccountRepository.save(CreditAccount.createFor(userId)));
    }
}
