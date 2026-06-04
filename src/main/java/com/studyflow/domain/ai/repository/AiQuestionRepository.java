package com.studyflow.domain.ai.repository;

import com.studyflow.domain.ai.entity.AiQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    /**
     * 한 대화의 질문들을 첨부(이미지 파일 메타)까지 fetch join으로 한 번에 조회한다.
     *
     * <p>AI 호출 맥락(history) 구성용. ask/askStream은 의도적으로 트랜잭션 밖에서 동작하므로
     * LAZY인 attachments/fileAsset에 나중에 접근하면 LazyInitializationException이 난다.
     * 여기서 미리 함께 로딩한다. (컬렉션 fetch join이라 distinct로 부모 중복 제거)</p>
     */
    @Query("""
            SELECT DISTINCT q FROM AiQuestion q
            LEFT JOIN FETCH q.attachments a
            LEFT JOIN FETCH a.fileAsset
            WHERE q.conversation.id = :conversationId
            ORDER BY q.createdAt ASC
            """)
    List<AiQuestion> findWithAttachmentsByConversationId(@Param("conversationId") Long conversationId);

    /**
     * 아직 대화에 편입되지 않은 질문들을 한 페이지씩 가져온다. (기존 데이터 backfill용)
     *
     * <p>전체를 한 번에 로드하면 기존 질문이 많을 때 OOM 위험이 있어 배치로 처리한다.
     * 처리된 행은 조건(conversation IS NULL)에서 빠지므로 항상 첫 페이지만 반복 조회하면 된다.</p>
     */
    Page<AiQuestion> findByConversationIsNull(Pageable pageable);

    /**
     * 한 대화의 첨부 연결을 일괄 삭제한다. (대화 삭제용 1/2단계)
     *
     * <p>질문을 엔티티 단위로 N건 지우면 SELECT N + DELETE 2N 쿼리가 나가므로,
     * 첨부 → 질문 순서의 벌크 DELETE 2방으로 줄인다. (원본 file_asset은 보존)</p>
     */
    @Modifying
    @Query("""
            DELETE FROM AiQuestionAttachment a
            WHERE a.aiQuestion IN (SELECT q FROM AiQuestion q WHERE q.conversation.id = :conversationId)
            """)
    void deleteAttachmentsByConversationId(@Param("conversationId") Long conversationId);

    /** 한 대화의 질문들을 일괄 삭제한다. (대화 삭제용 2/2단계 — 첨부 삭제 후 호출) */
    @Modifying
    @Query("DELETE FROM AiQuestion q WHERE q.conversation.id = :conversationId")
    void deleteByConversationId(@Param("conversationId") Long conversationId);
}
