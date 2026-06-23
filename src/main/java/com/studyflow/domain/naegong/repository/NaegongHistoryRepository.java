package com.studyflow.domain.naegong.repository;

import com.studyflow.domain.naegong.entity.NaegongHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NaegongHistoryRepository extends JpaRepository<NaegongHistory, Long> {

    // 특정 사용자의 내공 이력을 최신순으로 조회 (GET /api/v1/users/{userId}/naegong-histories 용)
    List<NaegongHistory> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 테스트 데이터 정리 전용 — 프로덕션 서비스에서 직접 호출 금지
    void deleteAllByUserId(Long userId);
}
