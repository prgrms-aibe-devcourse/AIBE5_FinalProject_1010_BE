package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long>, QnaAnswerRepositoryCustom {

    long countByQuestionId(Long questionId);

    boolean existsByQuestionIdAndAccepted(Long questionId, boolean accepted);

    Optional<QnaAnswer> findFirstByQuestionIdAndAcceptedTrue(Long questionId);

    // 선생님 상세 페이지 활동 통계 — 작성자(author=User) 기준 답변 수 / 채택된 답변 수
    long countByAuthorId(Long authorId);

    long countByAuthorIdAndAcceptedTrue(Long authorId);

    // fetch join/집계 쿼리는 QnaAnswerRepositoryCustom(QueryDSL)에 정의되어 있다.

    /** 좋아요 토글 시 비관적 쓰기 락으로 조회 — likeCount 카운터 동시 갱신 방지. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM QnaAnswer a WHERE a.id = :id")
    Optional<QnaAnswer> findByIdWithLock(@Param("id") Long id);
}
