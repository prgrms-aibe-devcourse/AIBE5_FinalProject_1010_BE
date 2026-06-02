package com.studyflow.domain.subject.controller;

import com.studyflow.domain.subject.dto.response.SubjectResponse;
import com.studyflow.domain.subject.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 과목 API. (명세: GET /api/v1/subjects)
 *
 * <ul>
 *   <li>GET /api/v1/subjects — 수능 8개 대분류 과목 목록 조회</li>
 * </ul>
 *
 * <p>권한: 인증된 사용자(SecurityConfig의 anyRequest().authenticated()).
 * 비로그인 노출이 필요하면 PublicUrlProvider에 {@code /api/v1/subjects}를 추가하면 된다.
 * 응답은 기존 컨벤션대로 공통 래퍼 없이 DTO 목록을 그대로 반환한다.</p>
 */
@RestController
@RequestMapping("/api/v1/subjects")
@RequiredArgsConstructor
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    public List<SubjectResponse> getSubjects() {
        return subjectService.getSubjects();
    }
}
