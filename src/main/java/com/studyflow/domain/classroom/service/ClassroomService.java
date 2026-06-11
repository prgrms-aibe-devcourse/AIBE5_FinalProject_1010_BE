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
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        ClassroomSession session = sessionRepository
                .findTopByCourseIdAndStatusOrderByStartedAtDesc(courseId, ClassroomStatus.OPEN)
                .orElseGet(() -> sessionRepository.save(ClassroomSession.open(course)));

        // 선생님을 호스트 참가자로 등록(멱등) — canPublish=true
        ensureHostParticipant(session, teacherUserId);

        return ClassroomSessionResponse.from(session);
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
        session.close();

        // TODO: 종료가 "실제 강제 퇴장"이 되도록 LiveKit RoomService.DeleteRoom 호출 필요.
        //  현재는 DB status만 CLOSED로 바꾸므로, 이미 발급된 토큰(TTL 2h)을 든 클라이언트는
        //  LiveKit 서버에 직접 재연결할 수 있다. (서버 API 연동 전까지 TTL 단축으로 완화)
        return ClassroomCloseResponse.from(session);
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

        // canPublish는 선택값 — null이면 기존 값 유지
        boolean canPublish = request.canPublish() != null ? request.canPublish() : participant.isCanPublish();
        participant.updatePermissions(request.canDraw(), request.canShareScreen(), request.canChat(), canPublish);
        return ParticipantPermissionResponse.from(participant);
    }

    /**
     * LiveKit 토큰 발급 (22-4) — 수업 멤버만.
     * 송출 게이팅: 참가자의 canPublish(선생님 기본 true, 학생 기본 false)를 토큰에 반영.
     */
    @Transactional(readOnly = true)
    public LivekitTokenResponse issueLivekitToken(Long sessionId, Long userId, LivekitTokenRequest request) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("종료된 강의실의 토큰은 발급할 수 없습니다.");
        }

        boolean isHost = verifyMemberAndIsHost(session.getCourse(), userId);

        // 참가(22-3) 이력이 있으면 그 권한을, 없으면 호스트 여부로 기본 결정
        boolean canPublish = participantRepository.findBySessionIdAndUserId(sessionId, userId)
                .map(ClassroomParticipant::isCanPublish)
                .orElse(isHost);

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
