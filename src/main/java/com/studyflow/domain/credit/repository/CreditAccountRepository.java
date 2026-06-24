package com.studyflow.domain.credit.repository;

import com.studyflow.domain.credit.entity.CreditAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CreditAccountRepository extends JpaRepository<CreditAccount, Long> {
    Optional<CreditAccount> findByUserId(Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CreditAccount c WHERE c.userId = :userId")
    Optional<CreditAccount> findWithLockByUserId(@Param("userId") Long userId);
}
