package com.studyflow.domain.credit.repository;

import com.studyflow.domain.credit.entity.CreditHistory;
import com.studyflow.domain.credit.enums.CreditReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface CreditHistoryRepository extends JpaRepository<CreditHistory, Long> {
    Page<CreditHistory> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    @Query("SELECT c FROM CreditHistory c WHERE c.userId = :userId AND c.reason = :reason " +
           "AND (cast(:startDate as timestamp) IS NULL OR c.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR c.createdAt <= :endDate) " +
           "ORDER BY c.id DESC")
    Page<CreditHistory> findEarningsWithDates(
            @Param("userId") Long userId, 
            @Param("reason") CreditReason reason, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate, 
            Pageable pageable);

    @Query("SELECT COALESCE(SUM(c.amount), 0) FROM CreditHistory c WHERE c.userId = :userId AND c.reason = :reason " +
           "AND (cast(:startDate as timestamp) IS NULL OR c.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR c.createdAt <= :endDate)")
    Long sumEarningsWithDates(
            @Param("userId") Long userId, 
            @Param("reason") CreditReason reason, 
            @Param("startDate") LocalDateTime startDate, 
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT new com.studyflow.domain.admin.dto.AdminCreditHistoryResponse(" +
           "c.id, u.id, u.email, u.name, c.amount, c.reason, c.refId, c.balanceAfter, c.createdAt) " +
           "FROM CreditHistory c JOIN User u ON c.userId = u.id " +
           "WHERE (:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) " +
           "AND (cast(:startDate as timestamp) IS NULL OR c.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR c.createdAt <= :endDate) " +
           "AND (:reason IS NULL OR c.reason = :reason) " +
           "ORDER BY c.id DESC")
    Page<com.studyflow.domain.admin.dto.AdminCreditHistoryResponse> findAdminCreditHistories(
            @Param("email") String email,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("reason") CreditReason reason,
            Pageable pageable);

    @Query("SELECT c.reason, SUM(c.amount) " +
           "FROM CreditHistory c JOIN User u ON c.userId = u.id " +
           "WHERE (:email IS NULL OR u.email LIKE CONCAT('%', :email, '%')) " +
           "AND (cast(:startDate as timestamp) IS NULL OR c.createdAt >= :startDate) " +
           "AND (cast(:endDate as timestamp) IS NULL OR c.createdAt <= :endDate) " +
           "AND (:reason IS NULL OR c.reason = :reason) " +
           "GROUP BY c.reason")
    java.util.List<Object[]> getCreditSummary(
            @Param("email") String email,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("reason") CreditReason reason);
}
