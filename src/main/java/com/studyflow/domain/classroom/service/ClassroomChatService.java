package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.dto.response.ClassroomChatResponse;
import com.studyflow.domain.classroom.entity.ClassroomChat;
import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.exception.ClassroomNotOpenException;
import com.studyflow.domain.classroom.exception.ClassroomSessionNotFoundException;
import com.studyflow.domain.classroom.repository.ClassroomChatRepository;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.exception.UserNotFoundException;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 강의실 채팅 서비스 (apidetail.md 23장).
 *
 * <p>멤버십 검증(담당 선생님·ACTIVE 수강생)은 {@link ClassroomService#assertMember}를 재사용한다.
 * 실시간 전송은 WebSocket 컨트롤러가, 이력 조회는 REST 컨트롤러가 이 서비스를 호출한다.</p>
 */
@Service
@RequiredArgsConstructor
public class ClassroomChatService {

    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomChatRepository chatRepository;
    private final ClassroomService classroomService;
    private final UserRepository userRepository;

    /**
     * 강의실 채팅 전송 (23-2) — 수업 멤버만. 열린 세션에만 전송 가능.
     */
    @Transactional
    public ClassroomChatResponse sendMessage(Long sessionId, Long userId, String content) {
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("메시지 내용이 비어 있습니다.");
        }

        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("종료된 강의실에는 메시지를 보낼 수 없습니다.");
        }
        classroomService.assertMember(session.getCourse(), userId);

        User sender = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(ErrorCode.USER_NOT_FOUND,
                        "사용자를 찾을 수 없습니다. (userId: " + userId + ")"));

        ClassroomChat saved = chatRepository.save(ClassroomChat.of(session, sender, content.trim()));
        return ClassroomChatResponse.from(saved);
    }

    /**
     * 강의실 채팅 이력 조회 (23-1) — 수업 멤버만. 시간순.
     */
    @Transactional(readOnly = true)
    public List<ClassroomChatResponse> getChats(Long sessionId, Long userId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        classroomService.assertMember(session.getCourse(), userId);

        return chatRepository.findBySessionIdWithSender(sessionId).stream()
                .map(ClassroomChatResponse::from)
                .toList();
    }
}
