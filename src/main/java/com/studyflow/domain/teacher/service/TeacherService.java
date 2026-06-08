package com.studyflow.domain.teacher.service;

import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.teacher.dto.TeacherVerificationRequest;
import com.studyflow.domain.teacher.dto.TeacherVerificationResponse;
import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.exception.InvalidVerificationFileException;
import com.studyflow.domain.teacher.exception.TeacherProfileNotFoundException;
import com.studyflow.domain.teacher.exception.VerificationAlreadyPendingException;
import org.springframework.dao.DataIntegrityViolationException;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.teacher.repository.TeacherVerificationRepository;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.course.repository.CourseRepository.TeacherCourseCount;
import com.studyflow.domain.teacher.dto.EnrollmentRequestSummaryResponse;
import com.studyflow.domain.teacher.dto.TeacherCardResponse;
import com.studyflow.domain.teacher.dto.TeacherCourseCardResponse;
import com.studyflow.domain.teacher.dto.TeacherDetailResponse;
import com.studyflow.domain.teacher.dto.TeacherProfileResponse;
import com.studyflow.domain.teacher.dto.TeacherProfileUpdateRequest;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherVerificationRepository teacherVerificationRepository;
    private final FileAssetRepository fileAssetRepository;

    // 검색 노출 기준 상태 — RECRUITING(모집 중) + IN_PROGRESS(수강 중)
    private static final List<CourseStatus> VISIBLE_STATUSES =
            List.of(CourseStatus.RECRUITING, CourseStatus.IN_PROGRESS);

    // 선생님 목록 조회 — keyword/minNaegong 필터 지원
    // keyword, minNaegong이 null이면 해당 조건을 무시합니다
    public Page<TeacherCardResponse> getTeacherList(String keyword, Integer minNaegong, Pageable pageable) {

        // 1단계: 선생님 목록 + user JOIN FETCH (필터 적용)
        // LIKE wildcard(%, _, !)를 escape 처리해 문자 그대로 검색
        String kw = (keyword != null && !keyword.isBlank()) ? escapeKeyword(keyword.trim()) : null;
        Page<TeacherProfile> profiles = teacherProfileRepository.findAllWithUserFiltered(kw, minNaegong, pageable);

        List<Long> teacherProfileIds = profiles.getContent().stream()
                .map(TeacherProfile::getId)
                .toList();

        if (teacherProfileIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2단계: 수업 수 일괄 조회 (N+1 방지) — teacherProfileId → count Map으로 변환
        Map<Long, Long> courseCounts = courseRepository
                .countCoursesByTeacherProfileIds(teacherProfileIds, VISIBLE_STATUSES)
                .stream()
                .collect(Collectors.toMap(
                        TeacherCourseCount::getTeacherProfileId,
                        TeacherCourseCount::getCount
                ));

        // 3단계: TeacherProfile → TeacherCardResponse 변환
        return profiles.map(profile ->
                TeacherCardResponse.of(profile, courseCounts.getOrDefault(profile.getId(), 0L))
        );
    }

    // LIKE escape 문자('!')를 먼저 escape한 뒤 %, _ 순으로 처리
    private String escapeKeyword(String raw) {
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    // 로그인한 선생님 본인의 프로필 조회
    public TeacherProfileResponse getMyProfile(Long userId) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        return new TeacherProfileResponse(profile);
    }

    // 로그인한 선생님 본인의 프로필 수정
    @Transactional
    public TeacherProfileResponse updateMyProfile(Long userId, TeacherProfileUpdateRequest request) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        profile.update(request.getAddress(), request.getAwards(), request.getCareer(),
                request.getEducation(), request.getIntroduction(), request.getTeachingStyle());

        return new TeacherProfileResponse(profile);
    }

    // 로그인한 선생님 본인의 수업 목록 조회 — status가 null이면 전체 반환, 페이지네이션 지원
    public Page<TeacherCourseCardResponse> getMyCourses(Long userId, CourseStatus status, Pageable pageable) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        return courseRepository
                .findWithSubjectByTeacherProfileIdAndStatus(profile.getId(), status, pageable)
                .map(TeacherCourseCardResponse::from);
    }

    // 본인 수업에 대한 수강 신청 목록 조회 — courseId/status 필터 옵션, 12개씩 페이지네이션
    public Page<EnrollmentRequestSummaryResponse> getEnrollmentRequests(
            Long userId, Long courseId, EnrollmentRequestStatus status, Pageable pageable) {

        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        TeacherProfile profile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        Page<EnrollmentRequest> requests = enrollmentRequestRepository
                .findByTeacherProfileId(profile.getId(), courseId, status, pageable);

        // N+1 방지 — 페이지 내 학생 user ID 모아서 StudentProfile 일괄 조회
        Set<Long> userIds = requests.getContent().stream()
                .map(r -> r.getUser().getId())
                .collect(Collectors.toSet());

        Map<Long, StudentProfile> studentProfileMap = studentProfileRepository
                .findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(sp -> sp.getUser().getId(), sp -> sp));

        return requests.map(r ->
                EnrollmentRequestSummaryResponse.of(r, studentProfileMap.get(r.getUser().getId())));
    }

    // 선생님 상세 조회 — /teachers/:id 페이지용
    public TeacherDetailResponse getTeacherDetail(Long teacherProfileId) {

        // 1단계: 선생님 프로필 + user JOIN FETCH
        TeacherProfile profile = teacherProfileRepository.findWithUserById(teacherProfileId)
                .orElseThrow(() -> new TeacherProfileNotFoundException(teacherProfileId));

        // 2단계: 해당 선생님의 공개 수업 목록 (subject JOIN FETCH)
        List<TeacherCourseCardResponse> courses = courseRepository
                .findWithSubjectByTeacherProfileId(teacherProfileId, VISIBLE_STATUSES)
                .stream()
                .map(TeacherCourseCardResponse::from)
                .toList();

        return TeacherDetailResponse.of(profile, courses);
    }

    // 선생님 본인 인증 신청 목록 조회 — 최신순 페이지네이션
    public Page<TeacherVerificationResponse> getMyVerifications(Long userId, Pageable pageable) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        return teacherVerificationRepository.findByUserId(userId, pageable)
                .map(TeacherVerificationResponse::from);
    }

    // 선생님 인증 요청 — 이미 PENDING 상태인 요청이 있으면 중복 요청 차단
    // 서비스 레벨 체크 후 DB unique 제약이 최종 방어선 (Race Condition 대비)
    @Transactional
    public Long requestVerification(Long userId, TeacherVerificationRequest request) {
        User user = userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 선생님 프로필 존재 여부 확인
        teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        // 서비스 레벨 중복 체크 (빠른 실패) — DB unique 제약이 최종 방어선
        if (teacherVerificationRepository.existsByUserIdAndStatus(userId, VerificationStatus.PENDING)) {
            throw new VerificationAlreadyPendingException();
        }

        // fileAssetId가 실제로 업로드된 파일인지, 본인이 업로드한 파일인지, 사용 가능한 상태인지 검증
        FileAsset fileAsset = fileAssetRepository.findByIdWithUploader(request.getFileAssetId())
                .orElseThrow(() -> new InvalidVerificationFileException(
                        ErrorCode.FILE_NOT_FOUND, "존재하지 않는 파일입니다."));
        if (!fileAsset.getUploader().getId().equals(userId)) {
            throw new InvalidVerificationFileException(
                    ErrorCode.NOT_MY_FILE, "본인이 업로드한 파일만 사용할 수 있습니다.");
        }
        if (!fileAsset.isUsable()) {
            throw new InvalidVerificationFileException(
                    ErrorCode.FILE_NOT_FOUND, "삭제되었거나 업로드가 완료되지 않은 파일입니다.");
        }

        TeacherVerification verification = TeacherVerification.create(
                user,
                request.getDocumentType(),
                fileAsset.getFileUrl(),
                request.getDescription()
        );

        try {
            return teacherVerificationRepository.save(verification).getId();
        } catch (DataIntegrityViolationException e) {
            // uk_teacher_verification_user_processed 위반만 예상 (동시 PENDING 중복 요청)
            throw new VerificationAlreadyPendingException();
        }
    }
}
