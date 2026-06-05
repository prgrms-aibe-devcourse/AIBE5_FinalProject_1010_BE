package com.studyflow.domain.teacher.service;

import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.course.repository.CourseRepository.TeacherCourseCount;
import com.studyflow.domain.teacher.dto.TeacherCardResponse;
import com.studyflow.domain.teacher.dto.TeacherCourseCardResponse;
import com.studyflow.domain.teacher.dto.TeacherDetailResponse;
import com.studyflow.domain.teacher.dto.TeacherProfileResponse;
import com.studyflow.domain.teacher.dto.TeacherProfileUpdateRequest;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.global.exception.ErrorCode;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.exception.TeacherProfileNotFoundException;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;

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
}
