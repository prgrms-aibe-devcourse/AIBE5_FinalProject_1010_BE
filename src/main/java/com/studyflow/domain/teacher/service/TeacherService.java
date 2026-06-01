package com.studyflow.domain.teacher.service;

import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.teacher.dto.TeacherCardResponse;
import com.studyflow.domain.teacher.dto.TeacherCourseCardResponse;
import com.studyflow.domain.teacher.dto.TeacherDetailResponse;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository.TeacherCourseCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherProfileRepository teacherProfileRepository;
    private final CourseRepository courseRepository;

    // 선생님 목록 조회 — 메인 페이지 카드 슬라이드용
    // 최신 가입 선생님 순(createdAt DESC)은 Pageable에서 정렬 설정
    public Page<TeacherCardResponse> getTeacherList(Pageable pageable) {

        // 1단계: 선생님 목록 + user JOIN FETCH
        Page<TeacherProfile> profiles = teacherProfileRepository.findAllWithUser(pageable);

        List<Long> teacherProfileIds = profiles.getContent().stream()
                .map(TeacherProfile::getId)
                .toList();

        if (teacherProfileIds.isEmpty()) {
            return Page.empty(pageable);
        }

        // 2단계: 수업 수 일괄 조회 (N+1 방지) — courseId → count Map으로 변환
        Map<Long, Long> courseCounts = courseRepository
                .countCoursesByTeacherProfileIds(teacherProfileIds)
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

    // 선생님 상세 조회 — /teachers/:id 페이지용
    public TeacherDetailResponse getTeacherDetail(Long teacherProfileId) {

        // 1단계: 선생님 프로필 + user JOIN FETCH
        TeacherProfile profile = teacherProfileRepository.findWithUserById(teacherProfileId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "선생님을 찾을 수 없습니다."));

        // 2단계: 해당 선생님의 공개 수업 목록 (subject JOIN FETCH)
        List<TeacherCourseCardResponse> courses = courseRepository
                .findWithSubjectByTeacherProfileId(teacherProfileId)
                .stream()
                .map(TeacherCourseCardResponse::from)
                .toList();

        return TeacherDetailResponse.of(profile, courses);
    }
}
