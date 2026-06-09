package com.studyflow.domain.admin.service;

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
import com.studyflow.domain.admin.dto.UserCountResponse;
import com.studyflow.domain.admin.dto.UserCountStatisticsResponse;
import com.studyflow.domain.admin.entity.UserCountStatistics;
import com.studyflow.domain.admin.repository.UserCountStatisticsRepository;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

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

    // 선생님 인증 요청 목록 조회 — status가 null이면 전체 반환
    public Page<AdminVerificationSummaryResponse> getTeacherVerifications(
            VerificationStatus status, Pageable pageable) {
        return teacherVerificationRepository.findAllWithUser(status, pageable)
                .map(AdminVerificationSummaryResponse::from);
    }

    // 선생님 인증 요청 상세 조회
    public AdminVerificationDetailResponse getTeacherVerificationDetail(Long verificationId) {
        return teacherVerificationRepository.findByIdWithUser(verificationId)
                .map(AdminVerificationDetailResponse::from)
                .orElseThrow(() -> new VerificationNotFoundException(verificationId));
    }

    // 승인 대기 선생님 수 조회
    public UserCountResponse getVerificationPendingCount() {
        return new UserCountResponse(
                teacherVerificationRepository.countByStatus(VerificationStatus.PENDING));
    }

    // 수업 수 조회 — status가 null이면 전체 반환
    public UserCountResponse getCourseCount(CourseStatus status) {
        long count = (status == null)
                ? courseRepository.count()
                : courseRepository.countByStatus(status);
        return new UserCountResponse(count);
    }

    // 활성 회원 수 조회 — role이 null이면 전체 반환
    public UserCountResponse getUserCount(UserRole role) {
        long count = (role == null)
                ? userRepository.countByIsActiveTrue()
                : userRepository.countByIsActiveTrueAndRole(role);
        return new UserCountResponse(count);
    }

    // 비활성 회원 수 조회 (isActive=false, 탈퇴 안 함)
    public UserCountResponse getInactiveUserCount(UserRole role) {
        long count = (role == null)
                ? userRepository.countInactiveNonDeleted()
                : userRepository.countInactiveNonDeletedByRole(role);
        return new UserCountResponse(count);
    }

    // 탈퇴 회원 수 조회 (isDeleted != 0)
    public UserCountResponse getDeletedUserCount(UserRole role) {
        long count = (role == null)
                ? userRepository.countDeleted()
                : userRepository.countDeletedByRole(role);
        return new UserCountResponse(count);
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
        verification.getUser().verify();
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
}
