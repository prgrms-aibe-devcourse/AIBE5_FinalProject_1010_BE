package com.studyflow.domain.subscription.controller;

import com.studyflow.domain.subscription.enums.SubscriptionType;
import com.studyflow.domain.subscription.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 구독 상태/요금제 조회 API.
 * - GET /api/v1/subscriptions/me : 내 구독(타입별 활성 여부·만료일) + 요금제(가격·기간)
 */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> mySubscriptions(@AuthenticationPrincipal Long userId) {
        LocalDateTime now = LocalDateTime.now();
        var subs = subscriptionService.getMySubscriptions(userId);

        // 타입별 활성/만료 정보
        List<Map<String, Object>> active = subs.stream()
                .map(s -> Map.<String, Object>of(
                        "type", s.getType().name(),
                        "expiresAt", s.getExpiresAt(),
                        "active", s.isActive(now)
                ))
                .toList();

        // 요금제(가격·기간) — FE 구독 화면용
        List<Map<String, Object>> plans = Arrays.stream(SubscriptionType.values())
                .map(t -> Map.<String, Object>of(
                        "type", t.name(),
                        "name", t.getDisplayName(),
                        "price", t.getPrice(),
                        "durationDays", SubscriptionType.DURATION_DAYS
                ))
                .toList();

        return ResponseEntity.ok(Map.of("subscriptions", active, "plans", plans));
    }
}
