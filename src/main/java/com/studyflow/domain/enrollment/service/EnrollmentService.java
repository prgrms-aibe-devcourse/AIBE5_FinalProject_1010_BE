package com.studyflow.domain.enrollment.service;

import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.exception.EnrollmentDropException;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationCreatedEvent;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EnrollmentService {

    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void dropEnrollment(Long enrollmentId, Long userId) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Enrollment enrollment = enrollmentRepository.findByIdWithCourseAndUser(enrollmentId)
                .orElseThrow(() -> new EnrollmentDropException(
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
