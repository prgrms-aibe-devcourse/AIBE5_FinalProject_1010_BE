package com.studyflow.domain.teacher.controller;

import com.studyflow.domain.teacher.dto.*;

import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import java.util.Map;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.global.exception.ProfileAuthInfoException;
import com.studyflow.domain.teacher.dto.HotTeacherResponse;
import com.studyflow.domain.teacher.service.HotTeacherService;
import com.studyflow.domain.teacher.service.TeacherService;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.global.auth.controllerutil.CheckAuthInController;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

// 선생님 목록 및 상세 API — 비로그인 포함 전체 공개
@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Validated
public class TeacherController {

    private final TeacherService teacherService;
    private final HotTeacherService hotTeacherService;

    // 이번주 HOT 선생님 — 지난 7일 내공 획득 상위 TOP3 (비로그인 포함 공개). 메인 홈 노출용.
    // 주간 획득자가 3명 미만이면 전체기간 내공순으로 채워 항상 최대 3명을 반환한다.
    // 주의: 리터럴 경로 "/hot"이 "/{teacherProfileId}"보다 우선 매칭됨.
    @Operation(summary = "이번주 HOT 선생님 조회", description = "지난 7일 내공 획득량 상위 선생님 TOP3. 비로그인 공개.")
    @GetMapping("/hot")
    public ResponseEntity<List<HotTeacherResponse>> getWeeklyHotTeachers() {
        return ResponseEntity.ok(hotTeacherService.getWeeklyHotTeachers());
    }

    // 선생님 목록 — 검색/필터 지원 (이름·성별·나이·지역·대학교·과목 + 최신/오래된순)
    // 예시: GET /api/v1/teachers?keyword=홍길동&gender=MALE&minAge=20&maxAge=39
    //              &regions=서울 강남구&regions=경기 성남시&universities=서울대학교
    //              &subjectIds=1&subjectIds=3&sort=LATEST&page=0&size=12
    @Operation(summary = "선생님 목록 조회", description = "비로그인 포함 공개. isListed=true인 선생님만 반환. 키워드·지역·대학교·과목·정렬 필터 지원.")
    @GetMapping
    public ResponseEntity<Page<TeacherCardResponse>> getTeacherList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String gender,
            @PositiveOrZero(message = "최소 나이는 0 이상이어야 합니다.")
            @RequestParam(required = false) Integer minAge,
            @PositiveOrZero(message = "최대 나이는 0 이상이어야 합니다.")
            @RequestParam(required = false) Integer maxAge,
            @RequestParam(required = false) List<String> regions,
            @RequestParam(required = false) List<String> universities,
            @RequestParam(required = false) List<Long> subjectIds,
            @RequestParam(required = false, defaultValue = "LATEST") String sort,
            @PageableDefault(size = 12) Pageable pageable) {
        TeacherSearchCondition condition = new TeacherSearchCondition(
                keyword, gender, minAge, maxAge, regions, universities, subjectIds, sort);
        return ResponseEntity.ok(teacherService.getTeacherList(condition, pageable));
    }

    // 선생님 상세 페이지
    // 예시: GET /api/v1/teachers/1
    @Operation(summary = "선생님 상세 조회", description = "비로그인 포함 공개. isListed 여부와 무관하게 직접 URL 접근 시 조회 가능.")
    @GetMapping("/{teacherProfileId}")
    public ResponseEntity<TeacherDetailResponse> getTeacherDetail(@PathVariable Long teacherProfileId) {
        return ResponseEntity.ok(teacherService.getTeacherDetail(teacherProfileId));
    }

    // 선생님 마이페이지 관련 api

    // 로그인한 선생님 본인의 프로필 조회
    @Operation(summary = "내 프로필 조회", description = "선생님 전용. 본인의 TeacherProfile을 반환합니다.")
    @GetMapping("/me/profile")
    public ResponseEntity<?> getMyProfile(@AuthenticationPrincipal Long userId,
                                          Authentication authentication) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        TeacherProfileResponse response = teacherService.getMyProfile(userId);
        return ResponseEntity.ok(response);
    }

    // 로그인한 선생님 본인의 프로필 수정
    @Operation(summary = "내 프로필 수정", description = "선생님 전용. 주소·소개·수업 방식·전문 과목을 수정합니다.")
    @PatchMapping("/me/profile")
    public ResponseEntity<?> updateMyProfile(@AuthenticationPrincipal Long userId,
                                             Authentication authentication,
                                             @Valid @RequestBody TeacherProfileUpdateRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        TeacherProfileResponse response = teacherService.updateMyProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    // 로그인한 선생님 본인의 선생님 찾기 목록 노출 여부 토글
    // 예시: PATCH /api/v1/teachers/me/listed  body: { "listed": true }
    @Operation(summary = "선생님 찾기 목록 노출 여부 변경", description = "선생님 전용. true=검색 목록 노출, false=숨김. 첫 수업 등록 시 자동으로 true로 전환됩니다.")
    @PatchMapping("/me/listed")
    public ResponseEntity<?> updateMyListed(@AuthenticationPrincipal Long userId,
                                            Authentication authentication,
                                            @Valid @RequestBody TeacherListedUpdateRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        TeacherProfileResponse response = teacherService.updateMyListed(userId, request.getListed());
        return ResponseEntity.ok(response);
    }

    // 로그인한 선생님 본인의 수업 목록 조회
    // 예시: GET /api/v1/teachers/me/courses?status=RECRUITING&page=0&size=12
    @Operation(summary = "내 수업 목록 조회", description = "선생님 전용. status 파라미터로 RECRUITING/IN_PROGRESS/CLOSED 필터링 가능. 미지정 시 전체 반환.")
    @GetMapping("/me/courses")
    public ResponseEntity<Page<TeacherCourseCardResponse>> getMyCourses(@AuthenticationPrincipal Long userId,
                                                                        Authentication authentication,
                                                                        @RequestParam(required = false) CourseStatus status,
                                                                        @PageableDefault(size = 12) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(teacherService.getMyCourses(userId, status, pageable));
    }

    // 본인의 수업에 대한 수강 신청 목록 조회
    // 예시: GET /api/v1/teachers/me/enrollment-requests?courseId=10&status=PENDING&page=0&size=12
    @Operation(summary = "내 수업 수강 신청 목록 조회", description = "선생님 전용. courseId·status 필터 지원. status: PENDING/APPROVED/REJECTED.")
    @GetMapping("/me/enrollment-requests")
    public ResponseEntity<Page<EnrollmentRequestSummaryResponse>> getEnrollmentRequests(
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) EnrollmentRequestStatus status,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(teacherService.getEnrollmentRequests(userId, courseId, status, pageable));
    }

    // 선생님 인증 요청
    // 예시: POST /api/v1/teachers/me/verifications
    @Operation(summary = "학력·신원 인증 요청", description = "선생님 전용. 인증 서류를 업로드하고 관리자 검토를 요청합니다.")
    @PostMapping("/me/verifications")
    public ResponseEntity<Map<String, Long>> requestVerification(@AuthenticationPrincipal Long userId,
                                                                 Authentication authentication,
                                                                 @Valid @RequestBody TeacherVerificationRequest request) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        Long verificationId = teacherService.requestVerification(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", verificationId));
    }

    // 선생님 본인 인증 신청 목록 조회
    // 예시: GET /api/v1/teachers/me/verifications?page=0&size=12
    @Operation(summary = "내 인증 신청 목록 조회", description = "선생님 전용. 본인이 제출한 인증 신청 내역을 최신순으로 반환합니다.")
    @GetMapping("/me/verifications")
    public ResponseEntity<Page<TeacherVerificationResponse>> getVerificationList(
            @AuthenticationPrincipal Long userId,
            Authentication authentication,
            @PageableDefault(size = 12, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        CheckAuthInController.checkAuth(userId, authentication, UserRole.TEACHER,
                ProfileAuthInfoException::new);

        return ResponseEntity.ok(teacherService.getMyVerifications(userId, pageable));
    }
}
