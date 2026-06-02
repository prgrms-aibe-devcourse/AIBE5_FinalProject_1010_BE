package com.studyflow.domain.ai.controller;

import com.studyflow.domain.ai.dto.request.AiQuestionCreateRequest;
import com.studyflow.domain.ai.dto.response.AiQuestionHistoryResponse;
import com.studyflow.domain.ai.dto.response.AiQuestionResponse;
import com.studyflow.domain.ai.service.AiQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

/**
 * AI 질문 API. (명세 §26)
 *
 * <ul>
 *   <li>POST /api/v1/ai/questions — 질문하고 AI 답변 받기(동기, 전체 응답)</li>
 *   <li>POST /api/v1/ai/questions/stream — 질문하고 AI 답변을 토큰 단위로 스트리밍(SSE)</li>
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
     * AI 질문 요청(스트리밍). 답변 조각을 SSE로 흘려보내고, 종료 후 done 이벤트로 저장된 기록을 전달한다.
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> askStream(
            @Valid @RequestBody AiQuestionCreateRequest request,
            @AuthenticationPrincipal Long userId
    ) {
        return aiQuestionService.askStream(userId, request);
    }

    /**
     * 내 AI 질문 기록 조회(최신순, 페이징).
     */
    @GetMapping
    public Page<AiQuestionHistoryResponse> getMyHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable
    ) {
        return aiQuestionService.getMyHistory(userId, pageable);
    }
}
