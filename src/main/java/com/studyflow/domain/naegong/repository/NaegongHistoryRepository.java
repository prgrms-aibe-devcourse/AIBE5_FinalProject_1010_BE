package com.studyflow.domain.naegong.repository;

import com.studyflow.domain.naegong.entity.NaegongHistory;
import com.studyflow.domain.naegong.enums.NaegongReason;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NaegongHistoryRepository extends JpaRepository<NaegongHistory, Long> {

    // 특정 사용자의 내공 이력을 최신순으로 조회 (GET /api/v1/users/{userId}/naegong-histories 용)
    List<NaegongHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 테스트 데이터 정리 전용 — 프로덕션 서비스에서 직접 호출 금지
    void deleteAllByUserId(Long userId);

    // 일일 지급 합계 조회 — 하루 최대 지급 한도 확인용 (reason + referenceId + startOfDay 이후 createdAt)
    @Query("SELECT COALESCE(SUM(h.scoreChange), 0) FROM NaegongHistory h " +
           "WHERE h.user.id = :userId AND h.reason = :reason " +
           "AND h.referenceId = :courseId AND h.createdAt >= :startOfDay")
    int sumScoreByUserIdAndReasonAndCourseIdSince(
            @Param("userId") Long userId,
            @Param("reason") NaegongReason reason,
            @Param("courseId") Long courseId,
            @Param("startOfDay") LocalDateTime startOfDay);
}
