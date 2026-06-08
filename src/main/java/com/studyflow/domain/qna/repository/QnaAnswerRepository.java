package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {

    /**
     * 질문 상세의 답변 목록. 채택된 답변 → 좋아요 많은 순 → 오래된 순으로 정렬한다(지식인 스타일).
     * 작성자(author)는 함께 fetch.
     */
    @Query("SELECT a FROM QnaAnswer a " +
            "JOIN FETCH a.author " +
            "WHERE a.question.id = :questionId " +
            "ORDER BY a.accepted DESC, a.likeCount DESC, a.createdAt ASC")
    List<QnaAnswer> findByQuestionIdWithAuthor(@Param("questionId") Long questionId);

    // 채택/답변 작성용 — question 함께 fetch (작성자 권한 확인에 question.author 필요)
    @Query("SELECT a FROM QnaAnswer a " +
            "JOIN FETCH a.question q " +
            "JOIN FETCH q.author " +
            "JOIN FETCH a.author " +
            "WHERE a.id = :id")
    Optional<QnaAnswer> findDetailById(@Param("id") Long id);

    long countByQuestionId(Long questionId);

    // 목록 화면용 — 여러 질문의 답변 개수를 한 번에 집계 (N+1 방지)
    @Query("SELECT a.question.id AS questionId, COUNT(a) AS cnt FROM QnaAnswer a " +
            "WHERE a.question.id IN :questionIds GROUP BY a.question.id")
    List<QuestionAnswerCount> countByQuestionIds(@Param("questionIds") List<Long> questionIds);

    boolean existsByQuestionIdAndAccepted(Long questionId, boolean accepted);

    interface QuestionAnswerCount {
        Long getQuestionId();
        long getCnt();
    }
}
