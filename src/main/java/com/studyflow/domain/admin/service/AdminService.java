package com.studyflow.domain.admin.service;

import com.studyflow.global.config.CacheConfig;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.admin.exception.StatisticsDateNotPastException;
import com.studyflow.domain.admin.exception.VerificationNotFoundException;
import com.studyflow.domain.admin.exception.VerificationNotPendingException;
import com.studyflow.domain.teacher.repository.TeacherVerificationRepository;
import com.studyflow.domain.admin.dto.AdminStudentDetailResponse;
import com.studyflow.domain.admin.dto.AdminTeacherDetailResponse;
import com.studyflow.domain.admin.dto.AdminUserDetailInterface;
import com.studyflow.domain.admin.dto.AdminUserDetailResponse;
import com.studyflow.domain.admin.dto.AdminUserSummaryResponse;
import com.studyflow.domain.admin.dto.AdminVerificationDetailResponse;
import com.studyflow.domain.admin.dto.AdminVerificationSummaryResponse;
import com.studyflow.domain.admin.dto.AdminCreditHistoryResponse;
import com.studyflow.domain.admin.dto.AdminCreditSummaryResponse;
import com.studyflow.domain.admin.dto.CountResponse;
import com.studyflow.domain.admin.dto.UserCountByRoleResponse;
import com.studyflow.domain.admin.dto.UserCountStatisticsResponse;
import com.studyflow.domain.admin.entity.UserCountStatistics;
import com.studyflow.domain.admin.repository.UserCountStatisticsRepository;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.credit.repository.CreditHistoryRepository;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.subscription.entity.UserSubscription;
import com.studyflow.domain.subscription.repository.UserSubscriptionRepository;
import com.studyflow.domain.teacher.exception.TeacherProfileNotFoundException;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationCreatedEvent;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AdminService {

    private final TeacherVerificationRepository teacherVerificationRepository;
    private final UserRepository userRepository;
    private final UserCountStatisticsRepository userCountStatisticsRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final CourseRepository courseRepository;
    private final CreditHistoryRepository creditHistoryRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final ApplicationEventPublisher eventPublisher;

    // 선생님 인증 요청 목록 조회 — status가 null이면 전체 반환
    public Page<AdminVerificationSummaryResponse> getTeacherVerifications(
            VerificationStatus status, Pageable pageable) {
        return teacherVerificationRepository.findAllWithUser(status, pageable)
                .map(AdminVerificationSummaryResponse::from);
    }

    // 선생님 인증 요청 상세 조회
    public AdminVerificationDetailResponse getTeacherVerificationDetail(Long verificationId) {
        TeacherVerification verification = teacherVerificationRepository.findByIdWithUser(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        TeacherProfile profile = teacherProfileRepository.findByUserId(verification.getUser().getId())
                .orElse(null);

        return AdminVerificationDetailResponse.from(verification, profile);
    }

    // 승인 대기 선생님 수 조회
    public CountResponse getVerificationPendingCount() {
        return new CountResponse(
                teacherVerificationRepository.countByStatus(VerificationStatus.PENDING));
    }

    // 수업 수 조회 — status가 null이면 전체 반환
    public CountResponse getCourseCount(CourseStatus status) {
        long count = (status == null)
                ? courseRepository.count()
                : courseRepository.countByStatus(status);
        return new CountResponse(count);
    }

    // 활성 회원 수 조회 — GROUP BY role 단일 쿼리 + 5분 캐시
    @Cacheable(cacheNames = CacheConfig.ADMIN_USER_COUNT, key = "'all'")
    public UserCountByRoleResponse getUserCount() {
        return toUserCountByRoleResponse(userRepository.countActiveGroupByRole());
    }

    // 비활성 회원 수 조회 (isActive=false, 탈퇴 안 함) — GROUP BY role 단일 쿼리 + 5분 캐시
    @Cacheable(cacheNames = CacheConfig.ADMIN_INACTIVE_USER_COUNT, key = "'all'")
    public UserCountByRoleResponse getInactiveUserCount() {
        return toUserCountByRoleResponse(userRepository.countInactiveNonDeletedGroupByRole());
    }

    // 탈퇴 회원 수 조회 (isDeleted != 0) — GROUP BY role 단일 쿼리 + 5분 캐시
    @Cacheable(cacheNames = CacheConfig.ADMIN_DELETED_USER_COUNT, key = "'all'")
    public UserCountByRoleResponse getDeletedUserCount() {
        return toUserCountByRoleResponse(userRepository.countDeletedGroupByRole());
    }

    // GROUP BY 쿼리 결과([role, count] 행 목록)를 UserCountByRoleResponse로 변환
    private UserCountByRoleResponse toUserCountByRoleResponse(List<Object[]> rows) {
        Map<UserRole, Long> map = new EnumMap<>(UserRole.class);
        for (Object[] row : rows) {
            map.put((UserRole) row[0], (Long) row[1]);
        }
        long student = map.getOrDefault(UserRole.STUDENT, 0L);
        long teacher = map.getOrDefault(UserRole.TEACHER, 0L);
        long admin   = map.getOrDefault(UserRole.ADMIN,   0L);
        return new UserCountByRoleResponse(student + teacher + admin, student, teacher, admin);
    }

    // 활성 회원 목록 조회
    public Page<AdminUserSummaryResponse> getActiveUsers(UserRole role, Pageable pageable) {
        Page<User> users = (role == null)
                ? userRepository.findByIsActiveTrue(pageable)
                : userRepository.findByIsActiveTrueAndRole(role, pageable);
        return users.map(AdminUserSummaryResponse::from);
    }

    // 비활성 회원 목록 조회
    public Page<AdminUserSummaryResponse> getInactiveUsers(UserRole role, Pageable pageable) {
        Page<User> users = (role == null)
                ? userRepository.findInactiveNonDeleted(pageable)
                : userRepository.findInactiveNonDeletedByRole(role, pageable);
        return users.map(AdminUserSummaryResponse::from);
    }

    // 탈퇴 회원 목록 조회
    public Page<AdminUserSummaryResponse> getDeletedUsers(UserRole role, Pageable pageable) {
        Page<User> users = (role == null)
                ? userRepository.findAllDeleted(pageable)
                : userRepository.findAllDeletedByRole(role, pageable);
        return users.map(AdminUserSummaryResponse::from);
    }

    // 회원 상세 조회 — 학생이면 StudentProfile, 선생님이면 TeacherProfile 포함
    public AdminUserDetailInterface getUserDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        return switch (user.getRole()) {
            case STUDENT -> AdminStudentDetailResponse.of(
                    user, studentProfileRepository.findByUserId(userId).orElse(null));
            case TEACHER -> AdminTeacherDetailResponse.of(
                    user, teacherProfileRepository.findByUserId(userId).orElse(null));
            case ADMIN -> AdminUserDetailResponse.from(user);
        };
    }

    // 특정 날짜의 유저 수 통계 조회 — 없으면 Users 테이블에서 직접 카운팅 후 저장
    @Transactional
    public UserCountStatisticsResponse getUserCountStatistics(LocalDate date) {
        if (!date.isBefore(LocalDate.now())) {
            throw new StatisticsDateNotPastException();
        }
        return userCountStatisticsRepository.findByDate(date)
                .map(UserCountStatisticsResponse::from)
                .orElseGet(() -> {
                    try {
                        UserCountStatistics statistics = UserCountStatistics.builder()
                                .date(date)
                                .newStudentCount(userRepository.countByRoleAndCreatedAtDate(UserRole.STUDENT.name(), date))
                                .newTeacherCount(userRepository.countByRoleAndCreatedAtDate(UserRole.TEACHER.name(), date))
                                .newAdminCount(userRepository.countByRoleAndCreatedAtDate(UserRole.ADMIN.name(), date))
                                .deletedStudentCount(userRepository.countByRoleAndDeletedAtDate(UserRole.STUDENT.name(), date))
                                .deletedTeacherCount(userRepository.countByRoleAndDeletedAtDate(UserRole.TEACHER.name(), date))
                                .deletedAdminCount(userRepository.countByRoleAndDeletedAtDate(UserRole.ADMIN.name(), date))
                                .build();
                        userCountStatisticsRepository.save(statistics);
                        return UserCountStatisticsResponse.from(statistics);
                    } catch (DataIntegrityViolationException e) {
                        // 동시 요청으로 인한 unique constraint 위반 — 이미 저장된 데이터를 재조회
                        return userCountStatisticsRepository.findByDate(date)
                                .map(UserCountStatisticsResponse::from)
                                .orElseThrow();
                    }
                });
    }

    // 선생님 인증 요청 수락 — verification APPROVED + user.isVerified = true
    @Transactional
    public void approveVerification(Long verificationId) {
        TeacherVerification verification = teacherVerificationRepository.findByIdWithUser(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new VerificationNotPendingException();
        }

        verification.process(VerificationStatus.APPROVED, null);

        User teacher = verification.getUser();
        teacher.verify();

        teacherProfileRepository.findByUserId(teacher.getId())
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(teacher.getId()))
                .updateVerifiedInfo(
                        verification.getAwards(),
                        verification.getCareer(),
                        verification.getMajor(),
                        verification.getAdmissionYear()
                );

        // 인증 수락 알림 → 해당 선생님에게 전송 (AFTER_COMMIT 리스너가 처리)
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                teacher.getId(),
                NotificationType.TEACHER_VERIFIED,
                "인증이 완료되었어요 🎉",
                "관리자 인증이 승인되었어요. 이제 수업 등록, 강의실 열기 등 모든 기능을 이용할 수 있어요!",
                null
        ));
    }

    // 선생님 인증 요청 거절
    @Transactional
    public void rejectVerification(Long verificationId, String rejectReason) {
        TeacherVerification verification = teacherVerificationRepository.findByIdWithUser(verificationId)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));

        if (verification.getStatus() != VerificationStatus.PENDING) {
            throw new VerificationNotPendingException();
        }

        verification.process(VerificationStatus.REJECTED, rejectReason);
    }

    // 결제 및 마일리지 변동 내역 조회
    public Page<AdminCreditHistoryResponse> getCreditHistories(String email, LocalDate startDate, LocalDate endDate, CreditReason reason, Pageable pageable) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59, 999999999) : null;
        Page<AdminCreditHistoryResponse> page = creditHistoryRepository.findAdminCreditHistories(email, startDateTime, endDateTime, reason, pageable);

        List<Long> courseIds = page.getContent().stream()
                .filter(res -> res.getRefId() != null &&
                        (res.getReason() == CreditReason.COURSE_OPEN ||
                         res.getReason() == CreditReason.ENROLLMENT_PAY ||
                         res.getReason() == CreditReason.ENROLLMENT_INCOME))
                .map(AdminCreditHistoryResponse::getRefId)
                .toList();

        List<Long> subscriptionIds = page.getContent().stream()
                .filter(res -> res.getRefId() != null &&
                        (res.getReason() == CreditReason.SUBSCRIPTION_PURCHASE ||
                         res.getReason() == CreditReason.REFUND))
                .map(AdminCreditHistoryResponse::getRefId)
                .toList();

        Map<Long, String> courseTitles = courseRepository.findAllById(courseIds).stream()
                .collect(Collectors.toMap(Course::getId, Course::getTitle));

        Map<Long, String> subNames = userSubscriptionRepository.findAllById(subscriptionIds).stream()
                .collect(Collectors.toMap(UserSubscription::getId, sub -> sub.getType().getDisplayName()));

        page.getContent().forEach(res -> {
            if (res.getRefId() == null) return;
            if (res.getReason() == CreditReason.COURSE_OPEN ||
                res.getReason() == CreditReason.ENROLLMENT_PAY ||
                res.getReason() == CreditReason.ENROLLMENT_INCOME) {
                res.setDetail("수업명: " + courseTitles.getOrDefault(res.getRefId(), "알 수 없음"));
            } else if (res.getReason() == CreditReason.SUBSCRIPTION_PURCHASE ||
                       res.getReason() == CreditReason.REFUND) {
                res.setDetail("구독권: " + subNames.getOrDefault(res.getRefId(), "알 수 없음"));
            }
        });

        return page;
    }

    // 결제/수익/사용액 통계 요약
    public AdminCreditSummaryResponse getCreditSummary(String email, LocalDate startDate, LocalDate endDate, CreditReason reason) {
        LocalDateTime startDateTime = (startDate != null) ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = (endDate != null) ? endDate.atTime(23, 59, 59, 999999999) : null;

        List<Object[]> rows = creditHistoryRepository.getCreditSummary(email, startDateTime, endDateTime, reason);
        long totalCharge = 0, totalIncome = 0, totalSpent = 0, totalRefund = 0;

        for (Object[] row : rows) {
            String rStr = String.valueOf(row[0]);
            long sum = (row[1] != null) ? ((Number) row[1]).longValue() : 0L;

            if ("CHARGE".equals(rStr)) {
                totalCharge += sum;
            } else if ("ENROLLMENT_INCOME".equals(rStr)) {
                totalIncome += sum;
            } else if ("REFUND".equals(rStr) || "CANCEL_ENROLLMENT".equals(rStr) || "CANCEL_INCOME".equals(rStr)) {
                totalRefund += sum;
            } else if (sum < 0) {
                totalSpent += Math.abs(sum);
            }
        }

        return new AdminCreditSummaryResponse(totalCharge, totalIncome, totalSpent, totalRefund);
    }
}
