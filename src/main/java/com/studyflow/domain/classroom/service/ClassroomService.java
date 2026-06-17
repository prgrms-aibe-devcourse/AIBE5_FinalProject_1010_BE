package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.dto.request.LivekitTokenRequest;
import com.studyflow.domain.classroom.dto.request.ParticipantPermissionUpdateRequest;
import com.studyflow.domain.classroom.dto.response.*;
import com.studyflow.domain.classroom.entity.ClassroomParticipant;
import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.enums.ClassroomStatus;
import com.studyflow.domain.classroom.exception.ClassroomForbiddenException;
import com.studyflow.domain.classroom.exception.ClassroomNotOpenException;
import com.studyflow.domain.classroom.exception.ClassroomParticipantNotFoundException;
import com.studyflow.domain.classroom.exception.ClassroomSessionNotFoundException;
import com.studyflow.domain.classroom.repository.ClassroomParticipantRepository;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.exception.CourseNotFoundException;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.notification.enums.NotificationType;
import com.studyflow.domain.notification.event.NotificationCreatedEvent;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 강의실(실시간 화상수업) 세션 서비스.
 *
 * <p>권한 규칙 (사용자 요구사항):
 * <ul>
 *   <li>강의실 <b>열기/종료/권한변경</b> = 그 수업의 담당 선생님만</li>
 *   <li>강의실 <b>조회/참가</b> = 담당 선생님 또는 ACTIVE 수강생만</li>
 * </ul>
 * 역할(TEACHER/STUDENT) 자체는 SecurityConfig에서 걸러지고,
 * 이 서비스는 "본인 수업인지 / 수강생인지" 소유권·멤버십을 검증한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ClassroomService {

    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomParticipantRepository participantRepository;
    private final CourseRepository courseRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final LiveKitTokenService liveKitTokenService;
    private final WhiteboardStateStore whiteboardStateStore;
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;

    /** 호스트(선생님)가 이 시간 동안 하트비트가 없으면 강의실을 자동 종료한다. */
    private static final long HOST_ABSENCE_LIMIT_SECONDS = 5 * 60; // 5분

    /**
     * 강의실 열기 (22-1) — 담당 선생님만.
     * 이미 열린(OPEN) 세션이 있으면 새로 만들지 않고 기존 세션을 반환한다(멱등).
     * 개설한 선생님은 곧바로 참가자(canPublish=true)로 등록해, 열기 직후 토큰을 요청해도
     * 송출 권한이 보장되도록 한다.
     */
    @Transactional
    public ClassroomSessionResponse openSession(Long courseId, Long teacherUserId) {
        // 동시성: course 행에 쓰기 락을 걸어 "열기" 더블클릭 시 OPEN 세션이 2개 생기는 경쟁을 막는다.
        Course course = courseRepository.findByIdForUpdate(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        assertHost(course, teacherUserId);

        Optional<ClassroomSession> existing = sessionRepository
                .findTopByCourseIdAndStatusOrderByStartedAtDesc(courseId, ClassroomStatus.OPEN);
        ClassroomSession session = existing
                .orElseGet(() -> sessionRepository.save(ClassroomSession.open(course)));

        // 선생님을 호스트 참가자로 등록(멱등) — canPublish=true
        ensureHostParticipant(session, teacherUserId);

        // "새로" 연 경우에만 ACTIVE 수강생에게 강의실 열림 알림(멱등 재오픈 시 중복 알림 방지)
        if (existing.isEmpty()) {
            notifyClassroomOpened(course);
        }

        return ClassroomSessionResponse.from(session);
    }

    /** 강의실 열림 → 그 수업의 ACTIVE 수강생에게 알림 발행(클릭 시 /classroom/{courseId}로 이동). */
    private void notifyClassroomOpened(Course course) {
        String title = "강의실이 열렸어요";
        String message = "'" + course.getTitle() + "' 강의실이 열렸어요. 지금 입장해 보세요.";
        for (Enrollment e : enrollmentRepository.findWithUserByCourseIdAndStatus(course.getId(), EnrollmentStatus.ACTIVE)) {
            eventPublisher.publishEvent(new NotificationCreatedEvent(
                    e.getUser().getId(), NotificationType.CLASSROOM_OPENED, title, message, course.getId()));
        }
    }

    /**
     * 신규 참가자 행 저장. 동시 더블클릭으로 두 요청이 동시에 insert하면
     * unique(session,user) 제약 위반(DataIntegrityViolationException)이 날 수 있으므로,
     * 그 경우 이미 생성된 행을 재조회해 멱등하게 반환한다(500 방지).
     */
    private ClassroomParticipant joinNewParticipant(ClassroomSession session, Long userId, boolean isHost) {
        try {
            User user = userRepository.getReferenceById(userId);
            return participantRepository.save(ClassroomParticipant.join(session, user, isHost));
        } catch (DataIntegrityViolationException e) {
            return participantRepository.findBySessionIdAndUserId(session.getId(), userId)
                    .orElseThrow(() -> e);
        }
    }

    // 개설 선생님의 참가자 행을 보장(이미 있으면 그대로 둔다)
    private void ensureHostParticipant(ClassroomSession session, Long teacherUserId) {
        participantRepository.findBySessionIdAndUserId(session.getId(), teacherUserId)
                .orElseGet(() -> participantRepository.save(
                        ClassroomParticipant.join(session, userRepository.getReferenceById(teacherUserId), true)));
    }

    /**
     * 현재 강의실 조회 (22-2) — 수업 멤버만.
     * 열린 세션이 없으면 404(CLASSROOM_NOT_FOUND).
     */
    @Transactional(readOnly = true)
    public ClassroomCurrentResponse getCurrentSession(Long courseId, Long userId) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new CourseNotFoundException(courseId));
        verifyMemberAndIsHost(course, userId);

        ClassroomSession session = sessionRepository
                .findTopByCourseIdAndStatusOrderByStartedAtDesc(courseId, ClassroomStatus.OPEN)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "현재 열려 있는 강의실이 없습니다. (courseId: " + courseId + ")"));

        return ClassroomCurrentResponse.from(session);
    }

    /**
     * 강의실 참가 (22-3) — 수업 멤버만. 재입장 시 기존 참가 행 재사용.
     */
    @Transactional
    public ClassroomParticipantResponse joinSession(Long sessionId, Long userId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));

        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("이미 종료된 강의실에는 참가할 수 없습니다.");
        }

        boolean isHost = verifyMemberAndIsHost(session.getCourse(), userId);

        ClassroomParticipant participant = participantRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseGet(() -> joinNewParticipant(session, userId, isHost));

        return ClassroomParticipantResponse.from(participant);
    }

    /**
     * 강의실 종료 (22-6) — 담당 선생님만.
     */
    @Transactional
    public ClassroomCloseResponse closeSession(Long sessionId, Long teacherUserId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        assertHost(session.getCourse(), teacherUserId);

        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("이미 종료된 강의실입니다.");
        }
        endSession(session, "host");
        return ClassroomCloseResponse.from(session);
    }

    /**
     * 세션을 실제로 종료하는 공통 처리(수동 종료 / 호스트 부재 자동종료 공용).
     * 1) status=CLOSED, 2) 화이트보드 메모리 정리, 3) 종료 이벤트 브로드캐스트(모든 참가자 자동 퇴장).
     *
     * @param reason "host"(선생님 수동 종료) | "host-absent"(호스트 5분 부재 자동종료)
     */
    private void endSession(ClassroomSession session, String reason) {
        session.close();
        // 화이트보드 권위 상태를 메모리에서 제거 — 종료된 세션 보드가 계속 쌓이지 않도록 정리.
        whiteboardStateStore.clear(session.getId());
        // 종료 이벤트 브로드캐스트 → 모든 클라이언트가 강의실에서 자동으로 나간다.
        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + session.getId() + "/events",
                Map.of("type", "closed", "reason", reason));
        // TODO: LiveKit RoomService.DeleteRoom로 미디어 룸도 강제 종료(서버 SDK 연동 전까지는
        //  클라이언트가 종료 이벤트를 받아 스스로 연결을 끊는 방식).
    }

    /**
     * 호스트(선생님) 하트비트 — 강의실에 접속해 있는 동안 주기적으로 호출.
     * 호스트가 호출하면 lastHostSeenAt을 갱신해 자동종료 타이머를 리셋한다.
     * 이미 종료된 세션이면 응답의 status=CLOSED로 알려 클라가 나가게 한다.
     *
     * @return 현재 세션 상태(OPEN/CLOSED) 문자열
     */
    @Transactional
    public String heartbeat(Long sessionId, Long userId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        boolean isHost = verifyMemberAndIsHost(session.getCourse(), userId);
        if (isHost && session.isOpen()) {
            session.touchHost();
        }
        return session.getStatus().name();
    }

    /**
     * 호스트 부재 자동종료 스윕 — 스케줄러가 주기적으로 호출.
     * 호스트 하트비트가 5분 넘게 끊긴 OPEN 세션을 강제 종료한다(선생님이 종료 버튼 없이 나가거나
     * 새로고침/크롬 종료/재부팅 등으로 사라진 경우 학생들도 자동 퇴장).
     */
    @Transactional
    public void autoCloseIdleSessions() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(HOST_ABSENCE_LIMIT_SECONDS);
        List<ClassroomSession> idle =
                sessionRepository.findByStatusAndLastHostSeenAtBefore(ClassroomStatus.OPEN, cutoff);
        for (ClassroomSession session : idle) {
            endSession(session, "host-absent");
        }
    }

    /**
     * 참가자 권한 변경 (22-5) — 담당 선생님만.
     */
    @Transactional
    public ParticipantPermissionResponse updatePermissions(
            Long participantId, Long teacherUserId, ParticipantPermissionUpdateRequest request) {
        ClassroomParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ClassroomParticipantNotFoundException(participantId));
        assertHost(participant.getSession().getCourse(), teacherUserId);

        // canPublish는 선택값 — null이면 기존 값 유지.
        // ⚠️ 주의: 현재 issueLivekitToken이 멤버 전원 canPublish=true로 발급하므로(양방향 과외 정책),
        //   여기서 저장하는 isCanPublish는 LiveKit 송출 권한에 반영되지 않는다(저장만 됨).
        //   웨비나형 송출 제한을 다시 도입할 때 issueLivekitToken이 isCanPublish를 읽도록 되돌리면
        //   이 API가 곧바로 효력을 갖는다. canDraw/canShareScreen/canChat은 정상 동작.
        boolean canPublish = request.canPublish() != null ? request.canPublish() : participant.isCanPublish();
        participant.updatePermissions(request.canDraw(), request.canShareScreen(), request.canChat(), canPublish);
        return ParticipantPermissionResponse.from(participant);
    }

    /**
     * LiveKit 토큰 발급 (22-4) — 수업 멤버만.
     * 양방향 과외이므로 멤버(담당 선생님·ACTIVE 수강생)는 전원 송출(canPublish=true) 가능하다.
     * — 학생이 실제로 수업에 참여 중인지(카메라·마이크) 서로 확인할 수 있어야 하므로 전원 발행 허용.
     * (대규모 웨비나처럼 송출을 제한해야 하면 canPublish 권한 필드로 다시 게이팅 가능: 22-5 권한변경.)
     */
    @Transactional(readOnly = true)
    public LivekitTokenResponse issueLivekitToken(Long sessionId, Long userId, LivekitTokenRequest request) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("종료된 강의실의 토큰은 발급할 수 없습니다.");
        }

        // 멤버십 검증(비멤버 403). 호스트 여부는 식별용으로만 사용.
        verifyMemberAndIsHost(session.getCourse(), userId);

        // 전원 송출 허용 — 멤버이면 누구나 카메라/마이크 발행 가능.
        // TODO(웨비나 모드): 송출 제한이 필요해지면 아래를 participant.isCanPublish 기반으로 되돌릴 것.
        //   그 전까지 22-5(updatePermissions)의 canPublish 변경은 토큰에 반영되지 않는다(저장만 됨).
        boolean canPublish = true;

        // 인증된 userId 기반이라 보통 존재하지만, 정합성 위해 404로 처리
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. (userId: " + userId + ")"));

        Long courseId = session.getCourse().getId();
        String roomName = "course-" + courseId + "-session-" + sessionId;
        String identity = "user-" + userId;
        String displayName = user.getName();
        String role = user.getRole() != null ? user.getRole().name() : null;

        String token = liveKitTokenService.createToken(roomName, identity, displayName, canPublish);

        return new LivekitTokenResponse(
                liveKitTokenService.getUrl(), roomName, token, identity, displayName, role);
    }

    /**
     * 화이트보드 현재 권위 상태 조회 — 수업 멤버(담당 선생님 또는 ACTIVE 수강생)만.
     * 인증만 된 사용자가 임의 세션 보드를 들여다보지 못하도록 멤버십을 검증한다.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getWhiteboardSnapshot(Long sessionId, Long userId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        verifyMemberAndIsHost(session.getCourse(), userId);
        return whiteboardStateStore.snapshot(sessionId);
    }

    // ── 권한 검증 헬퍼 ──

    /** 해당 수업의 담당 선생님인지 검증. 아니면 403. */
    private void assertHost(Course course, Long userId) {
        boolean isHost = teacherProfileRepository.findByUserId(userId)
                .map(tp -> course.getTeacherProfile().getId().equals(tp.getId()))
                .orElse(false);
        if (!isHost) {
            throw new ClassroomForbiddenException("해당 수업의 담당 선생님만 수행할 수 있습니다.");
        }
    }

    /**
     * 수업 멤버(담당 선생님 또는 ACTIVE 수강생)인지 검증하고, 호스트 여부를 반환한다. 멤버가 아니면 403.
     * 검증(실패 시 throw)과 분류(호스트 여부)를 함께 하므로 assertX가 아닌 verify...AndIsHost로 명명.
     * 채팅 등 다른 강의실 서비스에서도 재사용하므로 public.
     *
     * @return 담당 선생님(host)이면 true, ACTIVE 수강생이면 false
     */
    public boolean verifyMemberAndIsHost(Course course, Long userId) {
        boolean isHost = teacherProfileRepository.findByUserId(userId)
                .map(tp -> course.getTeacherProfile().getId().equals(tp.getId()))
                .orElse(false);
        if (isHost) {
            return true;
        }
        boolean enrolled = enrollmentRepository.existsByUserIdAndCourseIdAndStatus(
                userId, course.getId(), EnrollmentStatus.ACTIVE);
        if (!enrolled) {
            throw new ClassroomForbiddenException("해당 수업의 참여자가 아닙니다.");
        }
        return false;
    }
}
