package com.studyflow.domain.wrongnote.repository;

import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WrongAnswerNoteRepository extends JpaRepository<WrongAnswerNote, Long> {

    @Query(
            value = """
                    SELECT n FROM WrongAnswerNote n
                    LEFT JOIN n.subject s
                    WHERE n.owner.id = :ownerId
                      AND n.deletedAt IS NULL
                      AND (:subjectId IS NULL OR s.id = :subjectId)
                      AND (
                          :keyword IS NULL OR :keyword = ''
                          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(n.questionContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(COALESCE(n.answerContent, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(COALESCE(n.memo, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    SELECT COUNT(n) FROM WrongAnswerNote n
                    LEFT JOIN n.subject s
                    WHERE n.owner.id = :ownerId
                      AND n.deletedAt IS NULL
                      AND (:subjectId IS NULL OR s.id = :subjectId)
                      AND (
                          :keyword IS NULL OR :keyword = ''
                          OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(n.questionContent) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(COALESCE(n.answerContent, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                          OR LOWER(COALESCE(n.memo, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<WrongAnswerNote> searchMine(
            @Param("ownerId") Long ownerId,
            @Param("subjectId") Long subjectId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"owner", "subject", "tags"})
    Optional<WrongAnswerNote> findByIdAndOwnerIdAndDeletedAtIsNull(Long id, Long ownerId);

    @Query("""
            SELECT n FROM WrongAnswerNote n
            LEFT JOIN FETCH n.subject s
            WHERE n.owner.id = :ownerId
              AND n.deletedAt IS NULL
              AND (:subjectId IS NULL OR s.id = :subjectId)
              AND n.questionContent IS NOT NULL
              AND TRIM(n.questionContent) <> ''
            ORDER BY
              CASE
                WHEN n.nextReviewAt IS NULL THEN 0
                WHEN n.nextReviewAt <= :now THEN 1
                ELSE 2
              END ASC,
              n.difficultyScore DESC,
              COALESCE(n.lastReviewedAt, n.createdAt) ASC,
              n.createdAt ASC
            """)
    List<WrongAnswerNote> findPracticeRecommendations(
            @Param("ownerId") Long ownerId,
            @Param("subjectId") Long subjectId,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
