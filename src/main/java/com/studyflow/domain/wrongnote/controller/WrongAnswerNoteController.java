package com.studyflow.domain.wrongnote.controller;

import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteCreateRequest;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteUpdateRequest;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteResponse;
import com.studyflow.domain.wrongnote.service.WrongAnswerNoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@Tag(name = "오답노트", description = "학생/선생님 오답노트 CRUD API")
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
