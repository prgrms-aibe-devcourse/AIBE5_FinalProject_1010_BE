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
import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 수강 신청 생성 서비스 — 신청 저장 후 선생님-학생 채팅방 자동 개설
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

        // 대기 중인 신청이 있으면 중복 신청 차단
        if (enrollmentRequestRepository.existsByUserIdAndCourseIdAndStatus(
                studentUserId, courseId, EnrollmentRequestStatus.PENDING)) {
            throw new EnrollmentRequestAlreadyPendingException();
        }

        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        EnrollmentRequest enrollmentRequest = EnrollmentRequest.create(course, student, request);
        enrollmentRequestRepository.save(enrollmentRequest);

        // 채팅방 생성 — 같은 선생님-학생 조합 방이 이미 있으면 기존 방 반환 (idempotent)
        ChatRoomCreateRequest chatRoomRequest = new ChatRoomCreateRequest(courseId, teacherUserId, studentUserId);
        ChatRoomResponse chatRoom = chatService.createDirectRoom(studentUserId, chatRoomRequest);

        return EnrollmentRequestResponse.of(enrollmentRequest, chatRoom.getRoomId());
    }
}
