package com.studyflow.domain.ai.controller;

import com.studyflow.domain.ai.dto.response.ConversationDetailResponse;
import com.studyflow.domain.ai.dto.response.ConversationSummaryResponse;
import com.studyflow.domain.ai.service.AiQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 대화(Conversation) 조회 API.
 *
 * <ul>
 *   <li>GET /api/v1/ai/conversations?subjectId= — 내 대화 목록(사이드바 타이틀)</li>
 *   <li>GET /api/v1/ai/conversations/{id} — 대화 상세(질문+답변 전체)</li>
 * </ul>
 *
 * <p>대화 생성/이어쓰기는 질문 API(POST /ai/questions, /stream)에서 conversationId로 처리한다.</p>
 */
@RestController
@RequestMapping("/api/v1/ai/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final AiQuestionService aiQuestionService;

    /** 내 대화 목록(과목별, 최신순). subjectId 생략 시 전체 과목. */
    @GetMapping
    public List<ConversationSummaryResponse> getConversations(
            @RequestParam(required = false) Long subjectId,
            @AuthenticationPrincipal Long userId
    ) {
        return aiQuestionService.getConversations(userId, subjectId);
    }

    /** 대화 상세(질문+답변 전체). */
    @GetMapping("/{conversationId}")
    public ConversationDetailResponse getConversationDetail(
            @PathVariable Long conversationId,
            @AuthenticationPrincipal Long userId
    ) {
        return aiQuestionService.getConversationDetail(userId, conversationId);
    }
}
