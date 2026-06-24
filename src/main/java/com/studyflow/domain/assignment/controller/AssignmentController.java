package com.studyflow.domain.assignment.controller;

import com.studyflow.domain.assignment.dto.AssignmentRequest;
import com.studyflow.domain.assignment.dto.AssignmentResponse;
import com.studyflow.domain.assignment.service.AssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/courses/{courseId}/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @GetMapping
    public ResponseEntity<List<AssignmentResponse>> getAssignments(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(assignmentService.getAssignments(courseId, userId));
    }

    @PostMapping
    public ResponseEntity<AssignmentResponse> createAssignment(
            @PathVariable Long courseId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assignmentService.createAssignment(courseId, userId, req));
    }

    @PutMapping("/{assignmentId}")
    public ResponseEntity<AssignmentResponse> updateAssignment(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody AssignmentRequest req) {
        return ResponseEntity.ok(assignmentService.updateAssignment(courseId, assignmentId, userId, req));
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(
            @PathVariable Long courseId,
            @PathVariable Long assignmentId,
            @AuthenticationPrincipal Long userId) {
        assignmentService.deleteAssignment(courseId, assignmentId, userId);
        return ResponseEntity.noContent().build();
    }
}
