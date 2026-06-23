package com.studyflow.domain.classroom.service;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.exception.ClassroomForbiddenException;
import com.studyflow.domain.classroom.exception.ClassroomNotOpenException;
import com.studyflow.domain.classroom.exception.ClassroomSessionNotFoundException;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ClassroomQuizService {

    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomService classroomService;
    private final ClassroomQuizStateStore quizStateStore;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> start(Long sessionId, Long userId, String question, String answer, Integer durationSec) {
        ClassroomSession session = openSession(sessionId);
        boolean host = classroomService.verifyMemberAndIsHost(session.getCourse(), userId);
        if (!host) {
            throw new ClassroomForbiddenException("담당 선생님만 문제를 출제할 수 있습니다.");
        }
        Map<String, Object> snapshot = quizStateStore.start(sessionId, userId, question, answer, durationSec);
        userRepository.findById(userId).ifPresent(user -> snapshot.put("teacherName", user.getName()));
        return snapshot;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> submit(Long sessionId, Long userId, String quizId, String answer) {
        ClassroomSession session = openSession(sessionId);
        boolean host = classroomService.verifyMemberAndIsHost(session.getCourse(), userId);
        if (host) {
            throw new IllegalArgumentException("선생님은 답안을 제출할 수 없습니다.");
        }
        return quizStateStore.submit(sessionId, userId, quizId, answer);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> end(Long sessionId, Long userId) {
        ClassroomSession session = openSession(sessionId);
        boolean host = classroomService.verifyMemberAndIsHost(session.getCourse(), userId);
        if (!host) {
            throw new ClassroomForbiddenException("담당 선생님만 문제를 종료할 수 있습니다.");
        }
        return quizStateStore.end(sessionId);
    }

    public Map<String, Object> endIfSame(Long sessionId, String quizId) {
        return quizStateStore.endIfSame(sessionId, quizId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> snapshot(Long sessionId, Long userId) {
        ClassroomSession session = openSession(sessionId);
        boolean host = classroomService.verifyMemberAndIsHost(session.getCourse(), userId);
        return quizStateStore.snapshotFor(sessionId, userId, host);
    }

    private ClassroomSession openSession(Long sessionId) {
        ClassroomSession session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ClassroomSessionNotFoundException(
                        "강의실 세션을 찾을 수 없습니다. (sessionId: " + sessionId + ")"));
        if (!session.isOpen()) {
            throw new ClassroomNotOpenException("종료된 강의실에서는 문제풀이를 진행할 수 없습니다.");
        }
        return session;
    }
}
