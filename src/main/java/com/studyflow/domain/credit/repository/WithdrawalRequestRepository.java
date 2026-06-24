package com.studyflow.domain.credit.repository;

import com.studyflow.domain.credit.entity.WithdrawalRequest;
import com.studyflow.domain.credit.dto.WithdrawalResponseDto;
import com.studyflow.domain.credit.enums.WithdrawalStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    
    Page<WithdrawalRequest> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    @Query("SELECT new com.studyflow.domain.credit.dto.WithdrawalResponseDto(" +
           "w.id, u.id, u.email, u.name, w.amount, w.bankName, w.accountNumber, w.accountHolder, w.status, w.createdAt) " +
           "FROM WithdrawalRequest w JOIN User u ON w.userId = u.id " +
           "WHERE (:status IS NULL OR w.status = :status) " +
           "ORDER BY w.id DESC")
    Page<WithdrawalResponseDto> findAdminWithdrawals(@Param("status") WithdrawalStatus status, Pageable pageable);
}
