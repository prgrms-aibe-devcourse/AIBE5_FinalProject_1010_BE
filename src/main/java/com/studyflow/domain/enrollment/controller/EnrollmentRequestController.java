package com.studyflow.domain.enrollment.controller;

import com.studyflow.domain.enrollment.service.EnrollmentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/enrollment-requests")
@RequiredArgsConstructor
@Validated
public class EnrollmentRequestController {
    private final EnrollmentRequestService enrollmentRequestService;

     // 수강 신청 관련 API는 CourseDetailController에 포함되어 있음 (POST /api/v1/courses/{courseId}/enrollment-requests)
     // 추가적으로 수강 신청 상태 변경(승인/거절/취소) API가 필요하다면 여기에 구현 가능

    // 크레딧 결제로 즉시 수강 등록(신청=결제=확정). 학생 크레딧에서 수업료 차감, 선생님에게 90% 적립.
    @PostMapping("/courses/{courseId}/enroll")
    public ResponseEntity<Map<String, Object>> enrollWithCredit(@PathVariable Long courseId,
                                                                @AuthenticationPrincipal Long userId) {
        long balance = enrollmentRequestService.enrollByCredit(courseId, userId);
        return ResponseEntity.ok(Map.of("courseId", courseId, "creditBalance", balance));
    }

    // 수강 신청 취소 (본인이 신청한 수업에 한하여, status=PENDING일 때만 가능)
    @PatchMapping("/{requestId}/cancel")
    public ResponseEntity<?> cancelEnrollmentRequest(@PathVariable Long requestId,
                                                     @AuthenticationPrincipal Long userId) {
        enrollmentRequestService.cancelEnrollmentRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    // 수강 신청 수락
    @PatchMapping("/{requestId}/accept")
    public ResponseEntity<?> acceptEnrollmentRequest(@PathVariable Long requestId,
                                                    @AuthenticationPrincipal Long userId) {
        enrollmentRequestService.acceptEnrollmentRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

    // 수강 신청 거절
    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<?> rejectEnrollmentRequest(@PathVariable Long requestId,
                                                    @AuthenticationPrincipal Long userId) {
        enrollmentRequestService.rejectEnrollmentRequest(requestId, userId);
        return ResponseEntity.ok().build();
    }

}
