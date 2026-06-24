package com.studyflow.domain.enrollment.service;

import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.exception.EnrollmentDropException;
import com.studyflow.domain.enrollment.exception.EnrollmentNotFoundException;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationCreatedEvent;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    /** 탈퇴 불가 조건: RECRUITING 또는 IN_PROGRESS 수업에 ACTIVE 수강생이 존재하는 경우 */
    private static final List<CourseStatus> BLOCKING_COURSE_STATUSES =
            List.of(CourseStatus.RECRUITING, CourseStatus.IN_PROGRESS);

    private final EnrollmentRepository enrollmentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 선생님 회원탈퇴 가드 — 수강 중인 학생이 있는 활성 수업이 한 건이라도 있으면 true */
    @Transactional(readOnly = true)
    public boolean hasActiveStudentsForTeacher(Long teacherUserId) {
        return enrollmentRepository.existsByCourseTeacherProfileUserIdAndCourseStatusInAndStatus(
                teacherUserId, BLOCKING_COURSE_STATUSES, EnrollmentStatus.ACTIVE);
    }

    @Transactional
    public void dropEnrollment(Long enrollmentId, Long userId) {
        Enrollment enrollment = enrollmentRepository.findByIdWithCourseAndUser(enrollmentId)
                .orElseThrow(() -> new EnrollmentNotFoundException(
                        ErrorCode.ENROLLMENT_NOT_FOUND,
                        ErrorCode.ENROLLMENT_NOT_FOUND.getMessage()));

        if (!enrollment.getUser().getId().equals(userId)) {
            throw new EnrollmentDropException(
                    ErrorCode.NOT_MY_ENROLLMENT,
                    ErrorCode.NOT_MY_ENROLLMENT.getMessage());
        }

        if (enrollment.getStatus() != EnrollmentStatus.ACTIVE) {
            throw new EnrollmentDropException(
                    ErrorCode.CANNOT_DROP_ENROLLMENT,
                    ErrorCode.CANNOT_DROP_ENROLLMENT.getMessage());
        }

        enrollment.cancel();

        Long teacherUserId = enrollment.getCourse().getTeacherProfile().getUser().getId();
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                teacherUserId, NotificationType.ENROLLMENT_DROPPED,
                "수강 중도 포기",
                String.format("%s님이 '%s' 수업을 중도 포기했어요.",
                        enrollment.getUser().getName(), enrollment.getCourse().getTitle()),
                enrollmentId));
    }
}
