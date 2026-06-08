package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswerLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QnaAnswerLikeRepository extends JpaRepository<QnaAnswerLike, Long> {

    boolean existsByAnswerIdAndUserId(Long answerId, Long userId);

    Optional<QnaAnswerLike> findByAnswerIdAndUserId(Long answerId, Long userId);

    // 질문 상세에서 현재 사용자가 좋아요한 답변 id 집합을 한 번에 조회 (liked 플래그 계산용)
    @Query("SELECT l.answer.id FROM QnaAnswerLike l " +
            "WHERE l.user.id = :userId AND l.answer.id IN :answerIds")
    List<Long> findLikedAnswerIds(@Param("userId") Long userId, @Param("answerIds") List<Long> answerIds);
}
