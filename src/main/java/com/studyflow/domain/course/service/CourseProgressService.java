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
    public CourseProgressResponse createProgress(Long courseId, Long userId, CourseProgressCreateRequest request) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        User author = course.getTeacherProfile().getUser(); // JOIN FETCH 된 teacher user 재활용
        LocalDate date = request.getProgressDate() != null ? request.getProgressDate() : LocalDate.now();
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
