package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.dto.request.LivekitTokenRequest;
import com.studyflow.domain.classroom.dto.request.ParticipantPermissionUpdateRequest;
import com.studyflow.domain.classroom.dto.response.ClassroomParticipantResponse;
import com.studyflow.domain.classroom.dto.response.ClassroomSessionResponse;
import com.studyflow.domain.classroom.dto.response.LivekitTokenResponse;
import com.studyflow.domain.classroom.dto.response.ParticipantPermissionResponse;
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
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClassroomServiceTest {

    @Mock ClassroomSessionRepository sessionRepository;
    @Mock ClassroomParticipantRepository participantRepository;
    @Mock CourseRepository courseRepository;
    @Mock TeacherProfileRepository teacherProfileRepository;
    @Mock EnrollmentRepository enrollmentRepository;
    @Mock UserRepository userRepository;
    @Mock LiveKitTokenService liveKitTokenService;

    @InjectMocks ClassroomService classroomService;

    private static final Long COURSE_ID = 1L;
    private static final Long SESSION_ID = 100L;
    private static final Long TEACHER_PROFILE_ID = 10L;
    private static final Long HOST_USER_ID = 100L;     // 담당 선생님 userId
    private static final Long STUDENT_USER_ID = 200L;  // ACTIVE 수강생
    private static final Long OUTSIDER_USER_ID = 300L;  // 멤버 아님

    @Mock Course course;
    @Mock TeacherProfile teacherProfile;
    @Mock User user;

    @BeforeEach
    void setUp() {
        lenient().when(course.getId()).thenReturn(COURSE_ID);
        lenient().when(course.getTeacherProfile()).thenReturn(teacherProfile);
        lenient().when(teacherProfile.getId()).thenReturn(TEACHER_PROFILE_ID);
        // 담당 선생님만 프로필이 매칭됨
        lenient().when(teacherProfileRepository.findByUserId(HOST_USER_ID)).thenReturn(Optional.of(teacherProfile));
        lenient().when(teacherProfileRepository.findByUserId(STUDENT_USER_ID)).thenReturn(Optional.empty());
        lenient().when(teacherProfileRepository.findByUserId(OUTSIDER_USER_ID)).thenReturn(Optional.empty());
        // 학생은 ACTIVE 수강생
        lenient().when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(STUDENT_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(true);
        lenient().when(enrollmentRepository.existsByUserIdAndCourseIdAndStatus(OUTSIDER_USER_ID, COURSE_ID, EnrollmentStatus.ACTIVE)).thenReturn(false);
        lenient().when(courseRepository.findById(COURSE_ID)).thenReturn(Optional.of(course));
        // openSession은 동시성 제어를 위해 비관적 락 조회(findByIdForUpdate)를 사용한다.
        lenient().when(courseRepository.findByIdForUpdate(COURSE_ID)).thenReturn(Optional.of(course));
        lenient().when(userRepository.getReferenceById(anyLong())).thenReturn(user);
    }

    private ClassroomSession openSessionEntity() {
        ClassroomSession s = ClassroomSession.open(course);
        ReflectionTestUtils.setField(s, "id", SESSION_ID);
        return s;
    }

    // ===== openSession (22-1) =====

    @Test
    @DisplayName("열기: 이미 열린 세션이 있으면 새로 만들지 않고 기존 세션 반환(멱등)")
    void openSession_idempotent_returnsExisting() {
        ClassroomSession existing = openSessionEntity();
        when(sessionRepository.findTopByCourseIdAndStatusOrderByStartedAtDesc(COURSE_ID, ClassroomStatus.OPEN))
                .thenReturn(Optional.of(existing));
        when(participantRepository.findBySessionIdAndUserId(SESSION_ID, HOST_USER_ID))
                .thenReturn(Optional.of(ClassroomParticipant.join(existing, user, true)));

        ClassroomSessionResponse res = classroomService.openSession(COURSE_ID, HOST_USER_ID);

        assertThat(res.sessionId()).isEqualTo(SESSION_ID);
        verify(sessionRepository, never()).save(any()); // 새 세션 생성 안 함
    }

    @Test
    @DisplayName("열기: 열린 세션이 없으면 새로 생성하고 선생님을 호스트 참가자로 등록")
    void openSession_createsSessionAndRegistersHost() {
        when(sessionRepository.findTopByCourseIdAndStatusOrderByStartedAtDesc(COURSE_ID, ClassroomStatus.OPEN))
                .thenReturn(Optional.empty());
        when(sessionRepository.save(any(ClassroomSession.class))).thenAnswer(inv -> {
            ClassroomSession s = inv.getArgument(0);
            ReflectionTestUtils.setField(s, "id", SESSION_ID);
            return s;
        });
        when(participantRepository.findBySessionIdAndUserId(SESSION_ID, HOST_USER_ID)).thenReturn(Optional.empty());

        classroomService.openSession(COURSE_ID, HOST_USER_ID);

        verify(sessionRepository).save(any(ClassroomSession.class));
        // 호스트 참가자 등록 (canPublish=true 로 join)
        ArgumentCaptor<ClassroomParticipant> captor = ArgumentCaptor.forClass(ClassroomParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().isCanPublish()).isTrue();
    }

    @Test
    @DisplayName("열기: 담당 선생님이 아니면 403(ClassroomForbidden)")
    void openSession_notHost_throwsForbidden() {
        // 다른 선생님(프로필 id 불일치)
        TeacherProfile other = org.mockito.Mockito.mock(TeacherProfile.class);
        when(other.getId()).thenReturn(999L);
        when(teacherProfileRepository.findByUserId(HOST_USER_ID)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> classroomService.openSession(COURSE_ID, HOST_USER_ID))
                .isInstanceOf(ClassroomForbiddenException.class);
    }

    // ===== getCurrentSession (22-2) =====

    @Test
    @DisplayName("현재 조회: 열린 세션이 없으면 404(ClassroomSessionNotFound)")
    void getCurrentSession_noOpenSession_throwsNotFound() {
        when(sessionRepository.findTopByCourseIdAndStatusOrderByStartedAtDesc(COURSE_ID, ClassroomStatus.OPEN))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomService.getCurrentSession(COURSE_ID, HOST_USER_ID))
                .isInstanceOf(ClassroomSessionNotFoundException.class);
    }

    // ===== joinSession (22-3) =====

    @Test
    @DisplayName("참가: 재입장 시 기존 참가 행을 재사용하고 새로 저장하지 않음")
    void joinSession_reentry_reusesExisting() {
        ClassroomSession session = openSessionEntity();
        ClassroomParticipant existing = ClassroomParticipant.join(session, user, false);
        ReflectionTestUtils.setField(existing, "id", 5L);
        when(user.getId()).thenReturn(STUDENT_USER_ID);
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId(SESSION_ID, STUDENT_USER_ID)).thenReturn(Optional.of(existing));

        ClassroomParticipantResponse res = classroomService.joinSession(SESSION_ID, STUDENT_USER_ID);

        assertThat(res.participantId()).isEqualTo(5L);
        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("참가: 종료된 강의실이면 400(ClassroomNotOpen)")
    void joinSession_closedSession_throwsNotOpen() {
        ClassroomSession session = openSessionEntity();
        session.close(); // CLOSED 상태로 전환
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> classroomService.joinSession(SESSION_ID, STUDENT_USER_ID))
                .isInstanceOf(ClassroomNotOpenException.class);
    }

    @Test
    @DisplayName("참가: 수업 멤버가 아니면 403(ClassroomForbidden)")
    void joinSession_notMember_throwsForbidden() {
        ClassroomSession session = openSessionEntity();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> classroomService.joinSession(SESSION_ID, OUTSIDER_USER_ID))
                .isInstanceOf(ClassroomForbiddenException.class);
    }

    // ===== updatePermissions (22-5) =====

    @Test
    @DisplayName("권한변경: 참가자를 못 찾으면 ClassroomParticipantNotFound (세션 예외 아님)")
    void updatePermissions_participantNotFound_throwsParticipantNotFound() {
        when(participantRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> classroomService.updatePermissions(
                7L, HOST_USER_ID, new ParticipantPermissionUpdateRequest(true, false, true, null)))
                .isInstanceOf(ClassroomParticipantNotFoundException.class);
    }

    @Test
    @DisplayName("권한변경: canPublish가 null이면 기존 값 유지")
    void updatePermissions_nullCanPublish_keepsExisting() {
        ClassroomSession session = openSessionEntity();
        ClassroomParticipant participant = ClassroomParticipant.join(session, user, true); // host → canPublish=true
        ReflectionTestUtils.setField(participant, "id", 5L);
        when(participantRepository.findById(5L)).thenReturn(Optional.of(participant));

        ParticipantPermissionResponse res = classroomService.updatePermissions(
                5L, HOST_USER_ID, new ParticipantPermissionUpdateRequest(false, false, true, null));

        assertThat(res.canPublish()).isTrue(); // null 전달 → 기존 true 유지
        assertThat(res.canDraw()).isFalse();
    }

    // ===== issueLivekitToken (22-4) =====

    @Test
    @DisplayName("토큰: 참가 이력이 없으면 호스트 여부로 canPublish 폴백(선생님=true)")
    void issueLivekitToken_noParticipant_fallbackToHost() {
        ClassroomSession session = openSessionEntity();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId(SESSION_ID, HOST_USER_ID)).thenReturn(Optional.empty());
        when(userRepository.findById(HOST_USER_ID)).thenReturn(Optional.of(user));
        when(user.getName()).thenReturn("김선생");
        when(user.getRole()).thenReturn(UserRole.TEACHER);
        when(liveKitTokenService.createToken(anyString(), anyString(), anyString(), anyBoolean())).thenReturn("token-xyz");
        when(liveKitTokenService.getUrl()).thenReturn("wss://lk");

        LivekitTokenResponse res = classroomService.issueLivekitToken(SESSION_ID, HOST_USER_ID, new LivekitTokenRequest("WEB"));

        ArgumentCaptor<Boolean> canPublish = ArgumentCaptor.forClass(Boolean.class);
        verify(liveKitTokenService).createToken(eq("course-1-session-100"), eq("user-100"), eq("김선생"), canPublish.capture());
        assertThat(canPublish.getValue()).isTrue(); // 호스트 폴백
        assertThat(res.token()).isEqualTo("token-xyz");
        assertThat(res.role()).isEqualTo("TEACHER");
    }

    @Test
    @DisplayName("토큰: 학생 참가자의 canPublish=false가 토큰에 반영(송출 게이팅)")
    void issueLivekitToken_studentSubscribeOnly() {
        ClassroomSession session = openSessionEntity();
        ClassroomParticipant studentParticipant = ClassroomParticipant.join(session, user, false); // 학생 → canPublish=false
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));
        when(participantRepository.findBySessionIdAndUserId(SESSION_ID, STUDENT_USER_ID)).thenReturn(Optional.of(studentParticipant));
        when(userRepository.findById(STUDENT_USER_ID)).thenReturn(Optional.of(user));
        when(user.getName()).thenReturn("박학생");
        when(user.getRole()).thenReturn(UserRole.STUDENT);
        when(liveKitTokenService.createToken(anyString(), anyString(), anyString(), anyBoolean())).thenReturn("tok");
        when(liveKitTokenService.getUrl()).thenReturn("wss://lk");

        classroomService.issueLivekitToken(SESSION_ID, STUDENT_USER_ID, new LivekitTokenRequest("WEB"));

        ArgumentCaptor<Boolean> canPublish = ArgumentCaptor.forClass(Boolean.class);
        verify(liveKitTokenService).createToken(anyString(), anyString(), anyString(), canPublish.capture());
        assertThat(canPublish.getValue()).isFalse(); // 시청 전용
    }

    @Test
    @DisplayName("토큰: 종료된 강의실이면 400(ClassroomNotOpen)")
    void issueLivekitToken_closedSession_throwsNotOpen() {
        ClassroomSession session = openSessionEntity();
        session.close();
        when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> classroomService.issueLivekitToken(SESSION_ID, HOST_USER_ID, new LivekitTokenRequest("WEB")))
                .isInstanceOf(ClassroomNotOpenException.class);
    }
}
