package com.studyflow.domain.credit.repository;

import com.studyflow.domain.credit.entity.CreditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {
    Page<CreditHistory> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);
}
