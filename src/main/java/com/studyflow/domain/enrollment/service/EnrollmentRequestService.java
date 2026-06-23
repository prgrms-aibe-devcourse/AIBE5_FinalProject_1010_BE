package com.studyflow.domain.enrollment.service;

import com.studyflow.domain.chat.dto.request.ChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.response.ChatRoomResponse;
import com.studyflow.domain.chat.service.ChatService;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CourseStatus;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.credit.CreditPolicy;
import com.studyflow.domain.credit.enums.CreditReason;
import com.studyflow.domain.credit.service.CreditService;
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
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationCreatedEvent;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final CreditService creditService;

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

        // 선생님에게 새 수강 신청 알림 (커밋 이후 저장)
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                teacherUserId, NotificationType.ENROLLMENT_REQUESTED,
                "새 수강 신청",
                String.format("%s님이 '%s' 수업에 수강을 신청했어요.", student.getName(), course.getTitle()),
                saved.getId()));

        return EnrollmentRequestResponse.of(saved, chatRoom.getRoomId());
    }

    @Transactional
    public void acceptEnrollmentRequest(Long requestId, Long userId) {
        EnrollmentRequest request = validateAndGetPendingRequest(requestId, userId);

        // 수강 신청 수락 & Enrollment 생성
        request.accept();
        Enrollment enrollment = Enrollment.create(request.getUser(), request.getCourse(), request);
        enrollmentRepository.save(enrollment);

        // 학생에게 수락 알림
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                request.getUser().getId(), NotificationType.ENROLLMENT_ACCEPTED,
                "수강 신청 수락",
                String.format("'%s' 수강 신청이 수락되었어요.", request.getCourse().getTitle()),
                request.getId()));
    }

    /**
     * 크레딧 결제 기반 즉시 수강 등록(신청=결제=확정). 학생이 수강신청을 누르면 호출된다.
     *
     * <p>돈의 흐름: 학생 크레딧에서 수업료를 차감하고, 그중 수수료(10%)를 제외한 90%를
     * 선생님 크레딧으로 적립한다(나머지 10%는 플랫폼 수익). 차감·적립·수강등록을 한 트랜잭션으로
     * 묶어, 어느 하나라도 실패하면 전부 롤백된다.</p>
     *
     * @return 결제 후 학생의 크레딧 잔액
     */
    @Transactional
    public long enrollByCredit(Long courseId, Long studentUserId) {
        // teacherProfile → user 까지 페치(선생님 userId·수업료가 필요)
        Course course = courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        Long teacherUserId = course.getTeacherProfile().getUser().getId();
        User student = userRepository.findById(studentUserId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        validateEnrollable(course, studentUserId, teacherUserId);             // 결제 전 사전 검증
        long studentBalance = settleCreditPayment(course, studentUserId, teacherUserId); // 차감 + 정산
        Enrollment enrollment = confirmEnrollment(course, student);           // 즉시 수강 확정
        notifyTeacherOfEnrollment(course, student, teacherUserId, enrollment); // 선생님 알림

        return studentBalance;
    }

    /** 크레딧 결제 전 사전 검증(모집 상태·본인 수업·중복 수강). 돈이 빠지기 전에 막는다. */
    private void validateEnrollable(Course course, Long studentUserId, Long teacherUserId) {
        if (course.getStatus() != CourseStatus.RECRUITING) {
            throw new CourseNotRecruitingException();
        }
        if (teacherUserId.equals(studentUserId)) {
            throw new SelfEnrollmentException();
        }
        if (enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                studentUserId, course.getId(), EnrollmentStatus.ACTIVE)) {
            throw new AlreadyEnrolledException();
        }
    }

    /**
     * 수업료 결제·정산: 학생 크레딧 차감 → 수수료(10%) 제외한 90%를 선생님에게 적립.
     * 잔액 부족이면 InsufficientCreditException(충전 유도)으로 트랜잭션 롤백.
     *
     * @return 차감 후 학생 크레딧 잔액
     */
    private long settleCreditPayment(Course course, Long studentUserId, Long teacherUserId) {
        long price = course.getPricePerSession();
        if (price <= 0) {
            throw new CourseNotRecruitingException(); // 수업료가 비정상인 수업은 결제 진행하지 않음
        }
        long studentBalance = creditService.deduct(studentUserId, price, CreditReason.ENROLLMENT_PAY, course.getId());
        creditService.charge(teacherUserId, CreditPolicy.teacherIncome(price), CreditReason.ENROLLMENT_INCOME, course.getId());
        return studentBalance;
    }

    /** 즉시 수강 확정 — 자동 수락된 EnrollmentRequest와 그에 연결된 Enrollment를 생성한다. */
    private Enrollment confirmEnrollment(Course course, User student) {
        EnrollmentRequest request = EnrollmentRequest.create(
                course, student, null, null, null, null, "크레딧 결제로 자동 수강 등록");
        request.accept();
        enrollmentRequestRepository.save(request);

        Enrollment enrollment = Enrollment.create(student, course, request);
        return enrollmentRepository.save(enrollment);
    }

    /** 선생님에게 수강 등록 + 적립 수익 알림. */
    private void notifyTeacherOfEnrollment(Course course, User student, Long teacherUserId, Enrollment enrollment) {
        long teacherIncome = CreditPolicy.teacherIncome(course.getPricePerSession());
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                teacherUserId, NotificationType.ENROLLMENT_ACCEPTED,
                "새 수강 등록",
                String.format("%s님이 '%s' 수업을 결제하고 수강을 시작했어요. (+%d 크레딧)",
                        student.getName(), course.getTitle(), teacherIncome),
                enrollment.getId()));
    }

    @Transactional
    public void rejectEnrollmentRequest(Long requestId, Long userId) {
        EnrollmentRequest request = validateAndGetPendingRequest(requestId, userId);

        // 수강 신청 거절
        request.reject();

        // 학생에게 거절 알림
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                request.getUser().getId(), NotificationType.ENROLLMENT_REJECTED,
                "수강 신청 거절",
                String.format("'%s' 수강 신청이 거절되었어요.", request.getCourse().getTitle()),
                request.getId()));
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

        // enrollment_request 행에만 배타 락 (course·teacherProfile 행은 잠그지 않음)
        enrollmentRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new ProcessEnrollmentRequestException(
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND,
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND.getMessage()));
        // 락 획득 후 연관 엔티티를 JOIN FETCH로 로딩 (Hibernate 1차 캐시에서 같은 객체에 병합)
        EnrollmentRequest request = enrollmentRequestRepository.findByIdWithCourse(requestId)
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

        // 2. enrollment_request 행에만 배타 락 (조인 테이블은 잠그지 않음)
        enrollmentRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new EnrollmentRequestCancelException(
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND,
                        ErrorCode.ENROLLMENT_REQUEST_NOT_FOUND.getMessage()));
        // 락 획득 후 연관 엔티티를 JOIN FETCH로 로딩
        EnrollmentRequest request = enrollmentRequestRepository.findByIdWithUserAndCourse(requestId)
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

        // 선생님에게 취소 알림 (course → teacherProfile → user 는 트랜잭션 내 lazy 로딩)
        Long teacherUserId = request.getCourse().getTeacherProfile().getUser().getId();
        eventPublisher.publishEvent(new NotificationCreatedEvent(
                teacherUserId, NotificationType.ENROLLMENT_CANCELLED,
                "수강 신청 취소",
                String.format("%s님이 '%s' 수강 신청을 취소했어요.",
                        request.getUser().getName(), request.getCourse().getTitle()),
                request.getId()));
    }
}
