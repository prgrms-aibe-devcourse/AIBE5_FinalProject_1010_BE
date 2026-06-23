package com.studyflow.domain.wrongnote.controller;

import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteCreateRequest;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteReviewRequest;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteUpdateRequest;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteAnswerResponse;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNotePracticeResponse;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteReviewResponse;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteResponse;
import com.studyflow.domain.wrongnote.service.WrongAnswerNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Tag(name = "오답노트", description = "학생/선생님 오답노트 CRUD API")
@Validated
@RestController
@RequestMapping("/api/v1/wrong-answer-notes")
@RequiredArgsConstructor
public class WrongAnswerNoteController {

    private final WrongAnswerNoteService noteService;

    @Operation(summary = "내 오답노트 목록", description = "본인이 작성한 오답노트를 조회합니다. subjectId/keyword 필터를 지원합니다.")
    @GetMapping
    public ResponseEntity<Page<WrongAnswerNoteResponse>> getMyNotes(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(noteService.getMyNotes(userId, subjectId, keyword, pageable));
    }

    @Operation(summary = "내 오답노트 상세", description = "본인이 작성한 오답노트만 조회할 수 있습니다.")
    @GetMapping("/{noteId}")
    public ResponseEntity<WrongAnswerNoteResponse> getMyNote(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(noteService.getMyNote(noteId, userId));
    }

    @Operation(summary = "오답노트 문제풀이 추천", description = "복습 기록과 난이도 점수를 기준으로 문제만 추천합니다. 답안은 포함하지 않습니다.")
    @GetMapping("/practice/recommendations")
    public ResponseEntity<List<WrongAnswerNotePracticeResponse>> getPracticeRecommendations(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(noteService.getPracticeRecommendations(userId, subjectId, size));
    }

    @Operation(summary = "오답노트 작성", description = "직접 작성하거나 sourceType/sourceQuestionId/sourceAnswerId로 QnA/AI 질문 답변을 복사해 생성합니다.")
    @PostMapping
    public ResponseEntity<WrongAnswerNoteResponse> create(
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WrongAnswerNoteCreateRequest request
    ) {
        WrongAnswerNoteResponse response = noteService.create(userId, request);
        return ResponseEntity.created(URI.create("/api/v1/wrong-answer-notes/" + response.id()))
                .body(response);
    }

    @Operation(summary = "오답노트 수정", description = "본인이 작성한 오답노트만 수정할 수 있습니다.")
    @PatchMapping("/{noteId}")
    public ResponseEntity<WrongAnswerNoteResponse> update(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WrongAnswerNoteUpdateRequest request
    ) {
        return ResponseEntity.ok(noteService.update(noteId, userId, request));
    }

    @Operation(summary = "오답노트 문제풀이 답안 보기", description = "답안 보기 기록을 남긴 뒤 정답/풀이 정보를 반환합니다.")
    @PostMapping("/{noteId}/answer-view")
    public ResponseEntity<WrongAnswerNoteAnswerResponse> viewAnswer(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        return ResponseEntity.ok(noteService.viewAnswer(noteId, userId));
    }

    @Operation(summary = "오답노트 복습 결과 기록", description = "문제풀이 후 CORRECT/INCORRECT/UNSURE 결과를 기록하고 다음 복습 시점을 갱신합니다.")
    @PostMapping("/{noteId}/reviews")
    public ResponseEntity<WrongAnswerNoteReviewResponse> recordReview(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @Valid @RequestBody WrongAnswerNoteReviewRequest request
    ) {
        return ResponseEntity.ok(noteService.recordReview(noteId, userId, request));
    }

    @Operation(summary = "오답노트 복습 기록 목록", description = "본인 오답노트의 복습 기록을 최신순으로 조회합니다.")
    @GetMapping("/{noteId}/reviews")
    public ResponseEntity<Page<WrongAnswerNoteReviewResponse>> getReviewLogs(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "reviewedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(noteService.getReviewLogs(noteId, userId, pageable));
    }

    @Operation(summary = "오답노트 삭제", description = "본인이 작성한 오답노트만 삭제할 수 있습니다.")
    @DeleteMapping("/{noteId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long noteId,
            @Parameter(hidden = true) @AuthenticationPrincipal Long userId
    ) {
        noteService.delete(noteId, userId);
        return ResponseEntity.noContent().build();
    }
}
