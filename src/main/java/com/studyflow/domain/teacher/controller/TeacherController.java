package com.studyflow.domain.teacher.controller;

import com.studyflow.domain.teacher.dto.TeacherCardResponse;
import com.studyflow.domain.teacher.dto.TeacherDetailResponse;
import com.studyflow.domain.teacher.dto.TeacherProfileResponse;
import com.studyflow.domain.teacher.service.TeacherService;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// 선생님 목록 및 상세 API — 비로그인 포함 전체 공개
@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Validated
public class TeacherController {

    private final TeacherService teacherService;

    // 로그인한 선생님 본인의 프로필 조회
    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId) {
        if (userId == null) {
            Map<String, Object> body = Map.of(
                    "code", "AUTH_REQUIRED",
                    "message", "인증 정보가 유효하지 않습니다."
            );
            return ResponseEntity.status(401).body(body);
        }

        TeacherProfileResponse response = teacherService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 선생님 목록 — 검색/필터 지원
    // 예시: GET /api/v1/teachers?keyword=홍길동&minNaegong=500&page=0&size=12
    @GetMapping
    public ResponseEntity<Page<TeacherCardResponse>> getTeacherList(
            @RequestParam(required = false) String keyword,
            @PositiveOrZero(message = "내공 점수 하한은 0 이상이어야 합니다.")
            @RequestParam(required = false) Integer minNaegong,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(teacherService.getTeacherList(keyword, minNaegong, pageable));
    }

    // 선생님 상세 페이지
    // 예시: GET /api/v1/teachers/1
    @GetMapping("/{teacherProfileId}")
    public ResponseEntity<TeacherDetailResponse> getTeacherDetail(@PathVariable Long teacherProfileId) {
        return ResponseEntity.ok(teacherService.getTeacherDetail(teacherProfileId));
    }
}
