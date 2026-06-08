package com.studyflow.domain.qna.controller;

import com.studyflow.domain.qna.dto.request.QnaAnswerRequest;
import com.studyflow.domain.qna.dto.response.QnaAnswerAcceptResponse;
import com.studyflow.domain.qna.dto.response.QnaLikeResponse;
import com.studyflow.domain.qna.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.studyflow.domain.qna.controller.QnaAuthSupport.isAdmin;

/**
 * QnA 답변 관련 API. (수정/삭제/채택/좋아요/추가답변 작성)
 *
 * <ul>
 *   <li>PATCH/DELETE — 작성 선생님 본인(또는 관리자) — 서비스에서 소유권 검증</li>
 *   <li>PATCH /accept — 질문 작성 학생 본인 (SecurityConfig: STUDENT)</li>
 *   <li>POST /likes — 로그인 사용자 누구나 (authenticated)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/qna/answers")
@RequiredArgsConstructor
public class QnaAnswerController {

    private final QnaService qnaService;

    // 답변 수정 (작성 선생님 본인)
    @PatchMapping("/{answerId}")
    public ResponseEntity<Void> updateAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody QnaAnswerRequest request) {
        qnaService.updateAnswer(userId, answerId, request);
        return ResponseEntity.ok().build();
    }

    // 답변 삭제 (작성 선생님 본인 또는 관리자)
    @DeleteMapping("/{answerId}")
    public ResponseEntity<Void> deleteAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal Long userId,
            Authentication authentication) {
        qnaService.deleteAnswer(userId, answerId, isAdmin(authentication));
        return ResponseEntity.ok().build();
    }

    // 답변 채택 (질문 작성 학생 본인)
    @PatchMapping("/{answerId}/accept")
    public ResponseEntity<QnaAnswerAcceptResponse> acceptAnswer(
            @PathVariable Long answerId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(qnaService.acceptAnswer(userId, answerId));
    }

    // 답변 좋아요 토글 (로그인 사용자)
    @PostMapping("/{answerId}/likes")
    public ResponseEntity<QnaLikeResponse> toggleLike(
            @PathVariable Long answerId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(qnaService.toggleAnswerLike(userId, answerId));
    }
}
