package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.dto.response.LiveClassroomResponse;
import com.studyflow.domain.classroom.service.LiveClassroomQueryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 메인 홈 "실시간 강의중" 공개 목록 API.
 *
 * <p>비로그인 포함 전체 공개(PublicUrlProvider 등록). 지금 진행 중인 강의실 카드를 반환한다.</p>
 */
@Tag(name = "강의실", description = "실시간 화상수업 세션 API")
@RestController
@RequestMapping("/api/v1/live-classrooms")
@RequiredArgsConstructor
public class LiveClassroomController {

    private final LiveClassroomQueryService liveClassroomQueryService;

    @Operation(summary = "실시간 강의중 강의실 목록", description = "비로그인 공개. 지금 진행 중인 강의실 목록(최대 12개)을 반환합니다.")
    @GetMapping
    public ResponseEntity<List<LiveClassroomResponse>> getLiveClassrooms() {
        return ResponseEntity.ok(liveClassroomQueryService.getLiveClassrooms());
    }
}
