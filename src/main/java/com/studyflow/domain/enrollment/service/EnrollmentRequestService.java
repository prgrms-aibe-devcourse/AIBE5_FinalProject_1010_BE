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
import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.exception.AlreadyEnrolledException;
import com.studyflow.domain.enrollment.exception.CourseNotRecruitingException;
import com.studyflow.domain.enrollment.exception.ProcessEnrollmentRequestException;
import com.studyflow.domain.enrollment.exception.EnrollmentRequestAlreadyPendingException;
import com.studyflow.domain.enrollment.exception.EnrollmentRequestCancelException;
import com.studyflow.domain.enrollment.exception.SelfEnrollmentException;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.exception.TeacherProfileNotFoundException;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
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
    private final TeacherProfileRepository teacherProfileRepository;

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

    @Transactional
    public void acceptEnrollmentRequest(Long requestId, Long userId) {
        EnrollmentRequest request = validateAndGetPendingRequest(requestId, userId);

        // 수강 신청 수락 & Enrollment 생성
        request.accept();
        Enrollment enrollment = Enrollment.create(request.getUser(), request.getCourse(), request);
        enrollmentRepository.save(enrollment);
    }

    @Transactional
    public void rejectEnrollmentRequest(Long requestId, Long userId) {
        EnrollmentRequest request = validateAndGetPendingRequest(requestId, userId);

        // 수강 신청 거절
        request.reject();
    }

    /**
     * 수락/거절 공통 사전 검증 헬퍼
     * 1. 요청자가 선생님 프로필을 보유한 활성 계정인지 확인
     * 2. 수강 신청 기록 조회
     * 3. 본인 수업의 신청인지 확인
     * 4. PENDING 상태인지 확인
     */
    private EnrollmentRequest validateAndGetPendingRequest(Long requestId, Long userId) {
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        TeacherProfile teacherProfile = teacherProfileRepository.findByUserId(userId)
                .orElseThrow(() -> TeacherProfileNotFoundException.ofUserId(userId));

        EnrollmentRequest request = enrollmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProcessEnrollmentRequestException(
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND,
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND.getMessage()));

        if (!request.getCourse().getTeacherProfile().getId().equals(teacherProfile.getId())) {
            throw new ProcessEnrollmentRequestException(
                    ErrorCode.NOT_MY_COURSE_ENROLLMENT_REQUEST,
                    ErrorCode.NOT_MY_COURSE_ENROLLMENT_REQUEST.getMessage());
        }

        if (request.getStatus() != EnrollmentRequestStatus.PENDING) {
            throw new ProcessEnrollmentRequestException(
                    ErrorCode.CANNOT_PROCESS_ENROLLMENT_REQUEST,
                    ErrorCode.CANNOT_PROCESS_ENROLLMENT_REQUEST.getMessage());
        }

        return request;
    }

    @Transactional
    public void cancelEnrollmentRequest(Long requestId, Long userId) {
        // 1. 요청자 존재 여부 확인
        userRepository.findActiveById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 2. 수강 신청 기록 조회
        EnrollmentRequest request = enrollmentRequestRepository.findById(requestId)
                .orElseThrow(() -> new EnrollmentRequestCancelException(
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND,
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND.getMessage()));

        // 3. 본인 신청인지 확인
        if (!request.getUser().getId().equals(userId)) {
            throw new EnrollmentRequestCancelException(
                    ErrorCode.NOT_MY_ENROLLMENT_REQUEST,
                    ErrorCode.NOT_MY_ENROLLMENT_REQUEST.getMessage());
        }

        // 4. PENDING 상태인 경우에만 취소 가능
        //    ACCEPTED(수락됨), REJECTED(거절됨), CANCELLED(이미 취소됨) 모두 취소 불가
        if (request.getStatus() != EnrollmentRequestStatus.PENDING) {
            throw new EnrollmentRequestCancelException(
                    ErrorCode.CANNOT_CANCEL_ENROLLMENT_REQUEST,
                    ErrorCode.CANNOT_CANCEL_ENROLLMENT_REQUEST.getMessage());
        }

        // 5. 취소 처리
        request.cancel();
    }
}
