package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.progress.CourseProgressCreateRequest;
import com.studyflow.domain.course.dto.progress.CourseProgressResponse;
import com.studyflow.domain.course.dto.progress.CourseProgressUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.entity.CourseProgress;
import com.studyflow.domain.course.exception.CourseProgressNotFoundException;
import com.studyflow.domain.course.repository.CourseProgressRepository;
import com.studyflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

/**
 * 수업 진도 CRUD (이슈 #173).
 * 작성·수정·삭제는 담당 선생님 전용, 조회는 수업 멤버(담당 선생님·ACTIVE 수강생).
 */
@Service
@Transactional
@RequiredArgsConstructor
public class CourseProgressService {

    private final CourseProgressRepository progressRepository;
    private final CourseAccessValidator accessValidator;

    // 진도 목록 — 멤버 모두 조회 가능
    @Transactional(readOnly = true)
    public Page<CourseProgressResponse> getProgressList(Long courseId, Long userId, Pageable pageable) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        return progressRepository.findByCourseIdAndDeletedAtIsNull(courseId, pageable)
                .map(CourseProgressResponse::from);
    }

    // 진도 단건 조회 — 멤버 모두
    @Transactional(readOnly = true)
    public CourseProgressResponse getProgress(Long courseId, Long progressId, Long userId) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        CourseProgress progress = progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(progressId, courseId)
                .orElseThrow(() -> new CourseProgressNotFoundException(progressId));
        return CourseProgressResponse.from(progress);
    }

    // 진도 작성 — 담당 선생님 전용. progressDate 미지정 시 오늘 날짜.
    // 하나의 수업당 같은 날짜 진도는 1건만 둔다 — 같은 날짜가 이미 있으면 새로 만들지 않고
    // 그 항목 내용에 이어붙여 수정한다(이슈).
    public CourseProgressResponse createProgress(Long courseId, Long userId, CourseProgressCreateRequest request) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        LocalDate date = request.getProgressDate() != null ? request.getProgressDate() : LocalDate.now();

        Optional<CourseProgress> sameDate =
                progressRepository.findByCourseIdAndProgressDateAndDeletedAtIsNull(courseId, date);
        if (sameDate.isPresent()) {
            CourseProgress existing = sameDate.get();
            existing.appendContent(request.getContent()); // 같은 날짜 → 내용 이어붙임(dirty checking 저장)
            return CourseProgressResponse.from(existing);
        }

        User author = course.getTeacherProfile().getUser(); // JOIN FETCH 된 teacher user 재활용
        CourseProgress progress = CourseProgress.create(author, course, date, request.getContent());
        return CourseProgressResponse.from(progressRepository.save(progress));
    }

    // 진도 수정 — 담당 선생님 전용. progressDate 미지정 시 기존 날짜 유지.
    public CourseProgressResponse updateProgress(Long courseId, Long progressId, Long userId, CourseProgressUpdateRequest request) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        CourseProgress progress = progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(progressId, courseId)
                .orElseThrow(() -> new CourseProgressNotFoundException(progressId));
        LocalDate date = request.getProgressDate() != null ? request.getProgressDate() : progress.getProgressDate();

        // "수업당 같은 날짜 1건" 불변식 — 날짜를 바꾸는데 그 날짜에 이미 다른 진도가 있으면 거부(이슈 #179 리뷰).
        if (!date.equals(progress.getProgressDate())) {
            progressRepository.findByCourseIdAndProgressDateAndDeletedAtIsNull(courseId, date)
                    .filter(other -> !other.getId().equals(progressId))
                    .ifPresent(other -> {
                        throw new IllegalArgumentException("해당 날짜에는 이미 진도가 있어 날짜를 변경할 수 없습니다. 기존 진도를 수정해 주세요.");
                    });
        }

        progress.update(date, request.getContent());
        return CourseProgressResponse.from(progress);
    }

    // 진도 소프트 딜리트 — 담당 선생님 전용
    public void deleteProgress(Long courseId, Long progressId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        CourseProgress progress = progressRepository.findByIdAndCourseIdAndDeletedAtIsNull(progressId, courseId)
                .orElseThrow(() -> new CourseProgressNotFoundException(progressId));
        progress.delete();
    }
}
