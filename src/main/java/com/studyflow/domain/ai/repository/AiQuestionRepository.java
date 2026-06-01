package com.studyflow.domain.ai.repository;

import com.studyflow.domain.ai.entity.AiQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * AI 질문 기록 리포지토리.
 */
public interface AiQuestionRepository extends JpaRepository<AiQuestion, Long> {

    /**
     * 특정 사용자의 질문 기록을 최신순으로 조회한다.
     *
     * <p>기록 응답({@code AiQuestionHistoryResponse})이 과목 정보를 함께 내려주므로,
     * LAZY 연관인 {@code subject}를 fetch join으로 미리 로딩해 항목별 추가 SELECT(N+1)를 막는다.</p>
     */
    @Query("SELECT q FROM AiQuestion q JOIN FETCH q.subject WHERE q.user.id = :userId ORDER BY q.createdAt DESC")
    List<AiQuestion> findHistoryByUserId(@Param("userId") Long userId);
}
