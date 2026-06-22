package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestion;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long>, QnaQuestionRepositoryCustom {
    // 조회 쿼리는 QnaQuestionRepositoryCustom(QueryDSL)에 정의되어 있다.

    /** 해결 여부별 질문 수 (게시판 통계용). */
    long countByResolved(boolean resolved);

    /** 질문 삭제 / 답변 작성 시 비관적 쓰기 락으로 조회 — 동시 삭제·답변 작성 충돌 방지. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT q FROM QnaQuestion q WHERE q.id = :id")
    Optional<QnaQuestion> findByIdWithLock(@Param("id") Long id);
}
