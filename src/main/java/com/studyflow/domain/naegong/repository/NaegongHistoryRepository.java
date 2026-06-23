package com.studyflow.domain.naegong.repository;

import com.studyflow.domain.naegong.entity.NaegongHistory;
import org.springframework.data.domain.Pageable;
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

    // 이번주 HOT 선생님 — since 이후 user별 내공 변동 합산을 내림차순으로 조회 (메인 홈 TOP3용)
    // JPQL은 LIMIT을 못 쓰므로 Pageable(PageRequest.of(0, N))로 상위 N명만 가져온다.
    // 동점 시 user.id ASC로 결정적 정렬. idx_naegong_history_user_created 인덱스 활용.
    interface WeeklyNaegongGain {
        Long getUserId();
        Long getWeeklyScore();
    }

    @Query("SELECT h.user.id AS userId, SUM(h.scoreChange) AS weeklyScore " +
           "FROM NaegongHistory h " +
           "WHERE h.createdAt >= :since " +
           "GROUP BY h.user.id " +
           "ORDER BY SUM(h.scoreChange) DESC, h.user.id ASC")
    List<WeeklyNaegongGain> findWeeklyTopGainers(@Param("since") LocalDateTime since, Pageable pageable);
}
