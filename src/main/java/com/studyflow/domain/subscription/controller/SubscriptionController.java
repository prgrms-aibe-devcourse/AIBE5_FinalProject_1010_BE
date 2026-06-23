package com.studyflow.domain.subscription.controller;

import com.studyflow.domain.subscription.dto.SubscriptionPurchaseRequest;
import com.studyflow.domain.subscription.dto.SubscriptionSummaryResponse;
import com.studyflow.domain.subscription.dto.UserSubscriptionResponse;
import com.studyflow.domain.subscription.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @GetMapping("/me")
    public ResponseEntity<SubscriptionSummaryResponse> mySubscriptions(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(subscriptionService.getSummary(userId));
    }

    @PostMapping("/purchase")
    public ResponseEntity<UserSubscriptionResponse> purchase(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody SubscriptionPurchaseRequest request) {
        return ResponseEntity.ok(subscriptionService.purchase(userId, request.type()));
    }
}
