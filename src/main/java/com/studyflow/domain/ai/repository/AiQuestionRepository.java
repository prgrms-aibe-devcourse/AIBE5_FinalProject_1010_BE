package com.studyflow.domain.ai.repository;

import com.studyflow.domain.ai.entity.AiQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AI 질문 기록 리포지토리.
 */
public interface AiQuestionRepository extends JpaRepository<AiQuestion, Long> {

    /**
     * 특정 사용자의 질문 기록을 최신순으로 조회한다.
     *
     * <p>{@code user_Id}는 AiQuestion.user(User) 연관의 id를 따라가는 표현이다.
     * (= ai_question.user_id 컬럼 기준)</p>
     */
    List<AiQuestion> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
