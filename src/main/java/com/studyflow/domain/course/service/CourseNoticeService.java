package com.studyflow.domain.course.service;

import com.studyflow.domain.course.dto.notice.CourseNoticeCreateRequest;
import com.studyflow.domain.course.dto.notice.CourseNoticeResponse;
import com.studyflow.domain.course.dto.notice.CourseNoticeUpdateRequest;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.entity.CourseNotice;
import com.studyflow.domain.course.exception.CourseNoticeNotFoundException;
import com.studyflow.domain.course.repository.CourseNoticeRepository;
import com.studyflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class CourseNoticeService {

    private final CourseNoticeRepository noticeRepository;
    private final CourseAccessValidator accessValidator;

    // 공지 목록 — 선생님·수강생 모두 접근 가능
    @Transactional(readOnly = true)
    public Page<CourseNoticeResponse> getNotices(Long courseId, Long userId, Pageable pageable) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        return noticeRepository.findByCourseIdAndDeletedAtIsNull(courseId, pageable)
                .map(CourseNoticeResponse::from);
    }

    // 공지 단건 조회 — 선생님·수강생 모두 접근 가능
    @Transactional(readOnly = true)
    public CourseNoticeResponse getNotice(Long courseId, Long noticeId, Long userId) {
        accessValidator.validateParticipantAndGetCourse(courseId, userId);
        CourseNotice notice = noticeRepository.findByIdAndCourseIdAndDeletedAtIsNull(noticeId, courseId)
                .orElseThrow(() -> new CourseNoticeNotFoundException(noticeId));
        return CourseNoticeResponse.from(notice);
    }

    // 공지 작성 — 담당 선생님 전용
    public CourseNoticeResponse createNotice(Long courseId, Long userId, CourseNoticeCreateRequest request) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        // JOIN FETCH 된 teacher user 재활용 — 추가 쿼리 방지
        User author = course.getTeacherProfile().getUser();
        CourseNotice notice = CourseNotice.create(author, course, request.getTitle(), request.getContent(), request.isImportant());
        return CourseNoticeResponse.from(noticeRepository.save(notice));
    }

    // 공지 수정 — 담당 선생님 전용
    public CourseNoticeResponse updateNotice(Long courseId, Long noticeId, Long userId, CourseNoticeUpdateRequest request) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        CourseNotice notice = noticeRepository.findByIdAndCourseIdAndDeletedAtIsNull(noticeId, courseId)
                .orElseThrow(() -> new CourseNoticeNotFoundException(noticeId));
        notice.update(request.getTitle(), request.getContent(), request.isImportant());
        return CourseNoticeResponse.from(notice);
    }

    // 공지 소프트 딜리트 — 담당 선생님 전용
    public void deleteNotice(Long courseId, Long noticeId, Long userId) {
        Course course = accessValidator.validateParticipantAndGetCourse(courseId, userId);
        accessValidator.validateTeacher(course, userId);

        CourseNotice notice = noticeRepository.findByIdAndCourseIdAndDeletedAtIsNull(noticeId, courseId)
                .orElseThrow(() -> new CourseNoticeNotFoundException(noticeId));
        notice.delete();
    }
}
