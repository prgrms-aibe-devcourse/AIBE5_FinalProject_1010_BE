package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QnaQuestionRepository extends JpaRepository<QnaQuestion, Long> {

    /**
     * 질문 목록 조회 (필터: 과목 / 검색어(제목·내용) / 해결여부). null 파라미터는 조건에서 제외된다.
     * 목록 카드에 필요한 subject·author는 N+1 방지를 위해 함께 fetch 한다.
     */
    @Query(value = "SELECT q FROM QnaQuestion q " +
            "JOIN FETCH q.subject " +
            "JOIN FETCH q.author " +
            "WHERE (:subjectId IS NULL OR q.subject.id = :subjectId) " +
            "AND (:resolved IS NULL OR q.resolved = :resolved) " +
            "AND (:keyword IS NULL OR q.title LIKE CONCAT('%', :keyword, '%') OR q.content LIKE CONCAT('%', :keyword, '%'))",
            countQuery = "SELECT COUNT(q) FROM QnaQuestion q " +
                    "WHERE (:subjectId IS NULL OR q.subject.id = :subjectId) " +
                    "AND (:resolved IS NULL OR q.resolved = :resolved) " +
                    "AND (:keyword IS NULL OR q.title LIKE CONCAT('%', :keyword, '%') OR q.content LIKE CONCAT('%', :keyword, '%'))")
    Page<QnaQuestion> findFiltered(@Param("subjectId") Long subjectId,
                                   @Param("keyword") String keyword,
                                   @Param("resolved") Boolean resolved,
                                   Pageable pageable);

    // 상세 조회 — subject·author 함께 fetch
    @Query("SELECT q FROM QnaQuestion q " +
            "JOIN FETCH q.subject " +
            "JOIN FETCH q.author " +
            "WHERE q.id = :id")
    Optional<QnaQuestion> findDetailById(@Param("id") Long id);
}
