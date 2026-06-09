package com.studyflow.domain.qna.controller;

import com.studyflow.domain.qna.dto.request.QnaAnswerRequest;
import com.studyflow.domain.qna.dto.request.QnaQuestionCreateRequest;
import com.studyflow.domain.qna.dto.request.QnaQuestionUpdateRequest;
import com.studyflow.domain.qna.dto.response.QnaAnswerCreateResponse;
import com.studyflow.domain.qna.dto.response.QnaBoardStatsResponse;
import com.studyflow.domain.qna.dto.response.QnaQuestionCreateResponse;
import com.studyflow.domain.qna.dto.response.QnaQuestionDetailResponse;
import com.studyflow.domain.qna.dto.response.QnaQuestionSummaryResponse;
import com.studyflow.domain.qna.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.studyflow.domain.qna.controller.QnaAuthSupport.isAdmin;

/**
 * QnA 질문게시판 API. (apidetail.md 19장)
 *
 * <ul>
 *   <li>GET 목록/상세 — Public (비로그인 허용; PublicUrlProvider의 optionalAuth)</li>
 *   <li>POST 질문 — STUDENT (SecurityConfig 역할 규칙)</li>
 *   <li>POST 답변 — TEACHER (SecurityConfig 역할 규칙)</li>
 *   <li>PATCH/DELETE 질문 — 작성 학생 본인(또는 관리자) — 서비스에서 소유권 검증</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/qna/questions")
@RequiredArgsConstructor
public class QnaQuestionController {

    private final QnaService qnaService;

    // 질문 목록 조회 (Public)
    @GetMapping
    public ResponseEntity<Page<QnaQuestionSummaryResponse>> getQuestions(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean resolved,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(qnaService.getQuestions(subjectId, keyword, resolved, pageable));
    }

    // 질문게시판 전역 통계 (Public). '/{questionId}'보다 위에 둬 리터럴 경로가 우선 매칭되게 한다.
    @GetMapping("/stats")
    public ResponseEntity<QnaBoardStatsResponse> getBoardStats() {
        return ResponseEntity.ok(qnaService.getBoardStats());
    }

    // 질문 상세 조회 (Public). 로그인 시 좋아요 여부(liked) 계산을 위해 userId를 함께 받는다(없으면 null).
    @GetMapping("/{questionId}")
    public ResponseEntity<QnaQuestionDetailResponse> getQuestionDetail(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(qnaService.getQuestionDetail(questionId, userId));
    }

    // 질문 작성 (STUDENT)
    @PostMapping
    public ResponseEntity<QnaQuestionCreateResponse> createQuestion(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QnaQuestionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qnaService.createQuestion(userId, request));
    }

    // 질문 수정 (작성 학생 본인)
    @PatchMapping("/{questionId}")
    public ResponseEntity<Void> updateQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QnaQuestionUpdateRequest request) {
        qnaService.updateQuestion(userId, questionId, request);
        return ResponseEntity.ok().build();
    }

    // 질문 삭제 (작성 학생 본인 또는 관리자)
    @DeleteMapping("/{questionId}")
    public ResponseEntity<Void> deleteQuestion(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {
        qnaService.deleteQuestion(userId, questionId, isAdmin(authentication));
        return ResponseEntity.ok().build();
    }

    // 답변 작성 (TEACHER)
    @PostMapping("/{questionId}/answers")
    public ResponseEntity<QnaAnswerCreateResponse> createAnswer(
            @PathVariable Long questionId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QnaAnswerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(qnaService.createAnswer(userId, questionId, request));
    }
}
