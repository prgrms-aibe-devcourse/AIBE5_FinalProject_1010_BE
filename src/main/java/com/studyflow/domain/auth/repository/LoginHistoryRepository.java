package com.studyflow.domain.auth.repository;

import com.studyflow.domain.auth.entity.LoginHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    Page<LoginHistory> findByUserIdAndLoginAtAfterOrderByLoginAtDesc(
            Long userId, LocalDateTime after, Pageable pageable);
}
