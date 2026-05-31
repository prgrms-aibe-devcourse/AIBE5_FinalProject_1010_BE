package com.studyflow.domain.ai.controller;

import com.studyflow.domain.ai.dto.request.AiQuestionCreateRequest;
import com.studyflow.domain.ai.dto.response.AiQuestionHistoryResponse;
import com.studyflow.domain.ai.dto.response.AiQuestionResponse;
import com.studyflow.domain.ai.service.AiQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 질문 API. (명세 §26)
 *
 * <ul>
 *   <li>POST /api/v1/ai/questions — 질문하고 AI 답변 받기</li>
 *   <li>GET  /api/v1/ai/questions — 내 질문 기록 조회</li>
 * </ul>
 *
 * <p>권한: 인증된 사용자(SecurityConfig의 anyRequest().authenticated()).
 * 응답은 기존 컨벤션대로 공통 래퍼 없이 DTO를 그대로 반환한다.</p>
 *

 */
@RestController
@RequestMapping("/api/v1/ai/questions")
@RequiredArgsConstructor
public class AiQuestionController {

    private final AiQuestionService aiQuestionService;

    /**
     * AI 질문 요청. 생성된 기록을 201로 반환한다.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AiQuestionResponse ask(
            @Valid @RequestBody AiQuestionCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return aiQuestionService.ask(userId, request);
    }

    /**
     * 내 AI 질문 기록 조회(최신순).
     */
    @GetMapping
    public List<AiQuestionHistoryResponse> getMyHistory(
            @AuthenticationPrincipal Long userId
    ) {
        return aiQuestionService.getMyHistory(userId);
    }
}
