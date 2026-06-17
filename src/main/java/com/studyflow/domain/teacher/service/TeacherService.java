package com.studyflow.domain.teacher.service;

import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.teacher.dto.TeacherVerificationRequest;
import com.studyflow.domain.teacher.dto.TeacherVerificationResponse;
import com.studyflow.domain.teacher.entity.TeacherVerification;
import com.studyflow.domain.teacher.enums.VerificationStatus;
import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
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
import com.studyflow.domain.teacher.dto.TeacherSearchCondition;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
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
    private final SubjectRepository subjectRepository;
    private final QnaAnswerRepository qnaAnswerRepository;

    // 검색 노출 기준 상태 — RECRUITING(모집 중) + IN_PROGRESS(수강 중)
    private static final List<CourseStatus> VISIBLE_STATUSES =
            List.of(CourseStatus.RECRUITING, CourseStatus.IN_PROGRESS);

    // 선생님 목록 조회 — 이름/성별/나이/지역/대학교/과목 필터 + 최신·오래된순 정렬
    // 비어 있는 조건은 무시됩니다 (TeacherSearchCondition 참고)
    public Page<TeacherCardResponse> getTeacherList(TeacherSearchCondition condition, Pageable pageable) {

        // 이름 검색어 — LIKE wildcard(%, _, !)를 escape 처리해 문자 그대로 검색
        String kw = (condition.keyword() != null && !condition.keyword().isBlank())
                ? escapeKeyword(condition.keyword().trim()) : null;

        // 성별 — 유효한 enum 값만 적용, 그 외(null·오타)는 무시
        Gender gender = null;
        if (condition.gender() != null && !condition.gender().isBlank()) {
            try {
                gender = Gender.valueOf(condition.gender().trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // 알 수 없는 성별 값은 필터 미적용
            }
        }

        // 만 나이 범위 → 출생일 범위 변환
        //  · 만 minAge 이상  → 출생일 <= 오늘 - minAge년 (= birthTo)
        //  · 만 maxAge 이하  → 출생일 >= 오늘 - (maxAge+1)년 + 1일 (= birthFrom)
        LocalDate today = LocalDate.now();
        LocalDate birthTo   = (condition.minAge() != null) ? today.minusYears(condition.minAge()) : null;
        LocalDate birthFrom = (condition.maxAge() != null) ? today.minusYears(condition.maxAge() + 1L).plusDays(1) : null;

        // 빈/null 목록은 더미 값 + Empty 플래그로 IN 절을 무력화 (빈 컬렉션 IN 바인딩 오류 방지)
        boolean regionsEmpty      = isEmpty(condition.regions());
        boolean universitiesEmpty = isEmpty(condition.universities());
        boolean subjectsEmpty     = isEmpty(condition.subjectIds());
        List<String> regions      = regionsEmpty      ? List.of("")  : condition.regions();
        List<String> universities = universitiesEmpty ? List.of("")  : condition.universities();
        List<Long> subjectIds     = subjectsEmpty     ? List.of(-1L) : condition.subjectIds();

        // 정렬 — OLDEST면 createdAt 오름차순, 그 외(LATEST 기본)는 내림차순
        Sort sort = "OLDEST".equalsIgnoreCase(condition.sort())
                ? Sort.by(Sort.Direction.ASC, "createdAt")
                : Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable sortedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        // 1단계: 선생님 목록 + user JOIN FETCH (필터 적용)
        Page<TeacherProfile> profiles = teacherProfileRepository.findAllWithUserFiltered(
                kw, gender, birthFrom, birthTo,
                regionsEmpty, regions,
                universitiesEmpty, universities,
                subjectsEmpty, subjectIds,
                sortedPageable);

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

        // 3단계: 전문 과목명 일괄 조회 (N+1 방지) — teacherProfileId → 과목명 목록 Map으로 변환
        Map<Long, List<String>> specialtyMap = teacherProfileRepository
                .findSpecialtySubjectsByTeacherProfileIds(teacherProfileIds)
                .stream()
                .collect(Collectors.groupingBy(
                        TeacherProfileRepository.TeacherSpecialty::getTeacherProfileId,
                        Collectors.mapping(
                                TeacherProfileRepository.TeacherSpecialty::getSubjectName,
                                Collectors.toList())
                ));

        // 4단계: TeacherProfile → TeacherCardResponse 변환
        return profiles.map(profile ->
                TeacherCardResponse.of(
                        profile,
                        courseCounts.getOrDefault(profile.getId(), 0L),
                        specialtyMap.getOrDefault(profile.getId(), List.of()))
        );
    }

    // LIKE escape 문자('!')를 먼저 escape한 뒤 %, _ 순으로 처리
    private String escapeKeyword(String raw) {
        return raw.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    // null이거나 비어 있는 컬렉션인지 — 필터 조건 무시 여부 판단용
    private boolean isEmpty(Collection<?> c) {
        return c == null || c.isEmpty();
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

        profile.update(request.getAddress(), request.getIntroduction(), request.getTeachingStyle());

        // 전문 과목 — null이면 미변경, 그 외(빈 배열 포함)는 전달된 과목으로 교체
        if (request.getSpecialtySubjectIds() != null) {
            Set<Long> uniqueIds = request.getSpecialtySubjectIds().stream()
                    .collect(Collectors.toSet());
            if (!uniqueIds.isEmpty()) {
                List<Subject> subjects = subjectRepository.findAllById(uniqueIds);
                if (subjects.size() != uniqueIds.size()) {
                    Set<Long> foundIds = subjects.stream()
                            .map(Subject::getId).collect(Collectors.toSet());
                    Long missingId = uniqueIds.stream()
                            .filter(id -> !foundIds.contains(id)).findFirst().orElseThrow();
                    throw new SubjectNotFoundException(missingId);
                }
                profile.updateSpecialtySubjects(subjects);
            } else {
                profile.updateSpecialtySubjects(List.of());
            }
        }

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

        // 3단계: 질문게시판 활동 통계 (작성 답변 수 / 채택된 답변 수)
        Long teacherUserId = profile.getUser().getId();
        long answerCount   = qnaAnswerRepository.countByAuthorId(teacherUserId);
        long acceptedCount = qnaAnswerRepository.countByAuthorIdAndAcceptedTrue(teacherUserId);

        return TeacherDetailResponse.of(profile, courses, answerCount, acceptedCount);
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
                request.getDescription(),
                request.getAwards(),
                request.getCareer(),
                request.getMajor(),
                request.getAdmissionYear()
        );

        try {
            return teacherVerificationRepository.save(verification).getId();
        } catch (DataIntegrityViolationException e) {
            // uk_teacher_verification_user_processed 위반만 예상 (동시 PENDING 중복 요청)
            throw new VerificationAlreadyPendingException();
        }
    }
}
