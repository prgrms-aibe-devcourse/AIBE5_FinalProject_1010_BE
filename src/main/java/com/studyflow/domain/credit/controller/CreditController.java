package com.studyflow.domain.credit.controller;

import com.studyflow.domain.credit.CreditPolicy;
import com.studyflow.domain.credit.entity.CreditHistory;
import com.studyflow.domain.credit.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 크레딧 잔액/내역/정책 조회 API.
 * - GET /api/v1/credits/me          : 내 잔액 + 기능별 단가
 * - GET /api/v1/credits/me/history  : 내 크레딧 변동 내역(페이지)
 */
@RestController
@RequestMapping("/api/v1/credits")
@RequiredArgsConstructor
public class CreditController {

    private final CreditService creditService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> myCredit(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(Map.of(
                "balance", creditService.getBalance(userId),
                "costs", Map.of(
                        "aiQuestion", CreditPolicy.AI_QUESTION_COST,
                        "courseOpen", CreditPolicy.COURSE_OPEN_COST
                )
        ));
    }

    @GetMapping("/me/history")
    public ResponseEntity<Map<String, Object>> myHistory(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<CreditHistory> page = creditService.getHistory(userId, pageable);
        List<Map<String, Object>> items = page.getContent().stream()
                .map(h -> Map.<String, Object>of(
                        "id", h.getId(),
                        "amount", h.getAmount(),
                        "reason", h.getReason().name(),
                        "balanceAfter", h.getBalanceAfter(),
                        "createdAt", h.getCreatedAt()
                ))
                .toList();
        return ResponseEntity.ok(Map.of(
                "content", items,
                "totalElements", page.getTotalElements(),
                "totalPages", page.getTotalPages(),
                "page", page.getNumber()
        ));
    }
}
