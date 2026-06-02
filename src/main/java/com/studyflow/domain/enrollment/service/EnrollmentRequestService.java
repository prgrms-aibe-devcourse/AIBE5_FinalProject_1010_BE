package com.studyflow.domain.enrollment.service;

import com.studyflow.domain.chat.dto.request.ChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.response.ChatRoomResponse;
import com.studyflow.domain.chat.service.ChatService;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.dto.EnrollmentRequestCreateRequest;
import com.studyflow.domain.enrollment.dto.EnrollmentRequestResponse;
import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.exception.AlreadyEnrolledException;
import com.studyflow.domain.enrollment.exception.CourseNotRecruitingException;
import com.studyflow.domain.enrollment.exception.EnrollmentRequestAlreadyPendingException;
import com.studyflow.domain.enrollment.exception.SelfEnrollmentException;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 수강 신청 생성 서비스 — 채팅방 생성 후 신청 저장 순서로 원자성 보장
@Service
@RequiredArgsConstructor
public class EnrollmentRequestService {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentRequestRepository enrollmentRequestRepository;
    private final UserRepository userRepository;
    private final ChatService chatService;

    @Transactional
    public EnrollmentRequestResponse createEnrollmentRequest(
            Long courseId,
            Long studentUserId,
            EnrollmentRequestCreateRequest request
    ) {
        // teacherProfile → user 까지 페치 (채팅방 생성에 선생님 userId 필요)
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));

        // 모집 중인 수업에만 신청 가능
        if (course.getStatus() != CourseStatus.RECRUITING) {
            throw new CourseNotRecruitingException();
        }

        Long teacherUserId = course.getTeacherProfile().getUser().getId();

        // 선생님이 본인 수업에 신청하는 경우 차단
        if (teacherUserId.equals(studentUserId)) {
            throw new SelfEnrollmentException();
        }

        // 이미 수강 중이면 재신청 차단
        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(studentUserId, courseId, EnrollmentStatus.ACTIVE)) {
            throw new AlreadyEnrolledException();
        }

        // 서비스 레벨 중복 체크 (빠른 실패) — DB unique 제약이 최종 방어선
        if (enrollmentRequestRepository.existsByUserIdAndCourseIdAndStatus(
                studentUserId, courseId, EnrollmentRequestStatus.PENDING)) {
            throw new EnrollmentRequestAlreadyPendingException();
        }

        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // [원자성] 채팅방을 먼저 생성한다.
        // 채팅방 실패 시 → 신청 저장 안 됨 (정상 롤백)
        // 신청 저장 실패 시 → 빈 채팅방만 남음 (재신청 시 idempotent하게 재사용됨)
        ChatRoomCreateRequest chatRoomRequest = new ChatRoomCreateRequest(courseId, teacherUserId, studentUserId);
        ChatRoomResponse chatRoom = chatService.createDirectRoom(studentUserId, chatRoomRequest);

        EnrollmentRequest enrollmentRequest = EnrollmentRequest.create(
                course, student,
                request.getIntroduction(), request.getGoal(),
                request.getPreferredScheduleNote(), request.getPreferredStart(),
                request.getMessage()
        );

        EnrollmentRequest saved;
        try {
            saved = enrollmentRequestRepository.save(enrollmentRequest);
        } catch (DataIntegrityViolationException e) {
            // [동시성] 서비스 레벨 체크를 통과한 동시 요청이 DB unique 제약에 걸린 경우
            throw new EnrollmentRequestAlreadyPendingException();
        }

        return EnrollmentRequestResponse.of(saved, chatRoom.getRoomId());
    }
}
