package com.studyflow.domain.chat.service;

import com.studyflow.domain.chat.dto.request.CourseGroupChatParticipantRequest;
import com.studyflow.domain.chat.dto.request.CourseGroupChatRoomCreateRequest;
import com.studyflow.domain.chat.dto.response.ChatRoomResponse;
import com.studyflow.domain.chat.entity.ChatRoom;
import com.studyflow.domain.chat.entity.ChatRoomParticipant;
import com.studyflow.domain.chat.repository.ChatRoomParticipantRepository;
import com.studyflow.domain.chat.repository.ChatRoomRepository;
import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.entity.Enrollment;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseGroupChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomParticipantRepository chatRoomParticipantRepository;
    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ChatService chatService;
    private final ObjectProvider<CourseGroupChatService> selfProvider;

    /**
     * 수업 단체톡은 수업당 하나만 유지한다.
     * 이미 만들어진 방이 있으면 참여자만 보강하고 같은 방을 반환한다.
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public ChatRoomResponse createCourseGroupRoom(Long currentUserId, CourseGroupChatRoomCreateRequest request) {
        Course course = loadCourseWithTeacher(request.getCourseId());
        validateCourseTeacher(course, currentUserId);

        String roomKey = createCourseGroupRoomKey(course.getId());
        CourseGroupChatService self = selfProvider.getObject();
        RuntimeException lastError = null;

        for (int attempt = 0; attempt < 3; attempt++) {
            ChatRoomResponse existing = self.findCourseGroupRoomResponse(roomKey, currentUserId);
            if (existing != null) {
                return existing;
            }

            try {
                return self.createCourseGroupRoomInNewTx(course, roomKey, request.getStudentIds(), currentUserId);
            } catch (RuntimeException e) {
                lastError = e;
            }
        }

        ChatRoomResponse finalCheck = self.findCourseGroupRoomResponse(roomKey, currentUserId);
        if (finalCheck != null) {
            return finalCheck;
        }
        throw lastError;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public ChatRoomResponse findCourseGroupRoomResponse(String roomKey, Long currentUserId) {
        return chatRoomRepository.findByRoomKey(roomKey)
                .map(room -> chatService.toChatRoomResponse(room, currentUserId))
                .orElse(null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ChatRoomResponse createCourseGroupRoomInNewTx(Course course, String roomKey, List<Long> requestStudentIds, Long currentUserId) {
        ChatRoom newRoom = ChatRoom.createCourseGroupRoom(course, roomKey);
        ChatRoomParticipant.createTeacher(newRoom, course.getTeacherProfile().getUser());
        ChatRoom saved = chatRoomRepository.save(newRoom);

        List<Long> studentIds = requestStudentIds;
        if (studentIds == null || studentIds.isEmpty()) {
            studentIds = enrollmentRepository.findWithUserByCourseIdAndStatus(course.getId(), EnrollmentStatus.ACTIVE)
                    .stream()
                    .map(enrollment -> enrollment.getUser().getId())
                    .toList();
        }
        inviteStudents(saved, course, studentIds);

        return chatService.toChatRoomResponse(saved, currentUserId);
    }

    /**
     * 선생님이 자기 수업의 ACTIVE 수강생만 단체톡에 추가할 수 있다.
     */
    @Transactional
    public ChatRoomResponse inviteCourseGroupStudents(
            Long currentUserId,
            Long roomId,
            CourseGroupChatParticipantRequest request
    ) {
        ChatRoom room = loadCourseGroupRoomForTeacher(roomId, currentUserId);
        inviteStudents(room, room.getCourse(), request.getStudentIds());
        return chatService.toChatRoomResponse(room, currentUserId);
    }

    /**
     * 참여자 row는 삭제하지 않고 leave 처리해서 과거 메시지 이력을 보존한다.
     */
    @Transactional
    public ChatRoomResponse removeCourseGroupStudent(Long currentUserId, Long roomId, Long studentId) {
        ChatRoom room = loadCourseGroupRoomForTeacher(roomId, currentUserId);

        ChatRoomParticipant participant = chatRoomParticipantRepository
                .findByChatRoomIdAndUserIdAndLeftAtIsNull(roomId, studentId)
                .orElseThrow(() -> new IllegalArgumentException("현재 수업톡 참여 학생이 아닙니다."));

        if (!participant.isStudent()) {
            throw new IllegalArgumentException("선생님은 수업톡에서 내보낼 수 없습니다.");
        }

        participant.leave();
        return chatService.toChatRoomResponse(room, currentUserId);
    }

    private String createCourseGroupRoomKey(Long courseId) {
        return "COURSE_GROUP:COURSE:%d".formatted(courseId);
    }

    private Course loadCourseWithTeacher(Long courseId) {
        if (courseId == null) {
            throw new IllegalArgumentException("수업 ID는 필수입니다.");
        }
        return courseRepository.findWithTeacherAndSubjectById(courseId)
                .orElseThrow(() -> new IllegalArgumentException("수업을 찾을 수 없습니다."));
    }

    private ChatRoom loadCourseGroupRoomForTeacher(Long roomId, Long currentUserId) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));
        if (!room.isCourseGroupRoom() || room.getCourse() == null) {
            throw new IllegalArgumentException("수업 단체톡이 아닙니다.");
        }
        validateCourseTeacher(room.getCourse(), currentUserId);
        return room;
    }

    private void validateCourseTeacher(Course course, Long currentUserId) {
        Long teacherUserId = course.getTeacherProfile().getUser().getId();
        if (!teacherUserId.equals(currentUserId)) {
            throw new IllegalArgumentException("해당 선생님만 수업톡을 관리할 수 있습니다.");
        }
    }

    private void inviteStudents(ChatRoom room, Course course, List<Long> studentIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return;
        }

        List<Long> distinctStudentIds = new LinkedHashSet<>(studentIds).stream().toList();
        Map<Long, User> activeStudents = enrollmentRepository
                .findWithUserByCourseIdAndStatus(course.getId(), EnrollmentStatus.ACTIVE)
                .stream()
                .collect(Collectors.toMap(
                        enrollment -> enrollment.getUser().getId(),
                        Enrollment::getUser
                ));

        Map<Long, ChatRoomParticipant> existingParticipants = chatRoomParticipantRepository
                .findByChatRoomId(room.getId())
                .stream()
                .collect(Collectors.toMap(p -> p.getUser().getId(), p -> p));

        for (Long studentId : distinctStudentIds) {
            User student = activeStudents.get(studentId);
            if (student == null) {
                throw new IllegalArgumentException("해당 수업의 활성 수강생만 초대할 수 있습니다. (studentId: " + studentId + ")");
            }

            ChatRoomParticipant participant = existingParticipants.get(studentId);

            if (participant == null) {
                chatRoomParticipantRepository.save(ChatRoomParticipant.createStudent(room, student));
            } else if (!participant.isActive()) {
                participant.rejoin();
            }
        }
    }
}
