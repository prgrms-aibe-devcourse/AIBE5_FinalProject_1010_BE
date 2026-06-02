package com.studyflow.domain.ai.repository;

import com.studyflow.domain.ai.entity.AiQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * AI 질문 기록 리포지토리.
 */
public interface AiQuestionRepository extends JpaRepository<AiQuestion, Long> {

    /**
     * 특정 사용자의 질문 기록을 페이징하여 조회한다.
     *
     * <p>기록 응답({@code AiQuestionHistoryResponse})이 과목 정보를 함께 내려주므로,
     * LAZY 연관인 {@code subject}를 fetch join으로 미리 로딩해 항목별 추가 SELECT(N+1)를 막는다.
     * {@code subject}는 ToOne 연관이라 페이징과 함께 fetch join해도 행 중복/메모리 페이징 문제가 없다.
     * 정렬은 {@link Pageable}(기본 createdAt DESC)이 적용한다.</p>
     */
    @Query(
            value = "SELECT q FROM AiQuestion q JOIN FETCH q.subject WHERE q.user.id = :userId",
            countQuery = "SELECT COUNT(q) FROM AiQuestion q WHERE q.user.id = :userId"
    )
    Page<AiQuestion> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    /** 한 대화의 질문들을 시간순(오래된 것 → 최신)으로 조회한다. (대화 상세/복원용) */
    List<AiQuestion> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    /** 아직 대화에 편입되지 않은 질문들. (기존 데이터 backfill용) */
    List<AiQuestion> findByConversationIsNull();
}
