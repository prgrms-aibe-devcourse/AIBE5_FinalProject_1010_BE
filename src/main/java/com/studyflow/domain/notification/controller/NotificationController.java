package com.studyflow.domain.notification.controller;

import com.studyflow.domain.notification.dto.response.NotificationResponse;
import com.studyflow.domain.notification.dto.response.UnreadCountResponse;
import com.studyflow.domain.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림", description = "알림 조회·읽음 처리 API")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "내 알림 목록 조회", description = "최신순 페이징. 기본 20건.")
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long userId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(notificationService.getNotifications(userId, pageable));
    }

    @Operation(summary = "안 읽은 알림 수 조회", description = "네비게이션 바 벨 뱃지에 표시할 카운트.")
    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(new UnreadCountResponse(notificationService.countUnread(userId)));
    }

    @Operation(summary = "단건 읽음 처리", description = "본인 알림만 처리 가능. 타인 알림 접근 시 403.")
    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId,
                                           @AuthenticationPrincipal Long userId) {
        notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "전체 읽음 처리", description = "본인의 안 읽은 알림을 모두 읽음 처리.")
    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(@AuthenticationPrincipal Long userId) {
        notificationService.markAllAsRead(userId);
        return ResponseEntity.noContent().build();
    }
}
