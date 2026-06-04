package com.studyflow.domain.ai.repository;

import com.studyflow.domain.ai.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AI 대화(Conversation) 리포지토리.
 */
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    /** 내 대화 목록(과목별, 최신 생성순). 사이드바용. */
    List<Conversation> findByUserIdAndSubjectIdOrderByCreatedAtDesc(Long userId, Long subjectId);

    /** 내 대화 목록(전체 과목, 최신 생성순). */
    List<Conversation> findByUserIdOrderByCreatedAtDesc(Long userId);
}
