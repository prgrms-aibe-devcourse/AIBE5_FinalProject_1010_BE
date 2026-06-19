package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.entity.ClassroomSession;
import com.studyflow.domain.classroom.repository.ClassroomSessionRepository;
import com.studyflow.domain.classroom.service.ClassroomService;
import com.studyflow.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.Map;

/**
 * 강의실 리액션(손흔들기/좋아요) WebSocket 컨트롤러.
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/reactions — 구독: /sub/classroom-sessions/{sessionId}/reactions.
 * 화이트보드 live처럼 "휘발성" 메시지라 저장하지 않고 그대로 중계한다(보낸 사람·이름만 주입).
 * payload 예: {type:"like"|"hand"} → 전원에게 {type, senderId, senderName} 브로드캐스트.
 * 채팅/좋아요와 동일하게 수업 멤버만 허용(비멤버는 무시).</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomReactionWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final ClassroomService classroomService;

    // @Transactional: verifyMemberAndIsHost가 호스트 판정 시 course.getTeacherProfile()을 지연로딩하는데,
    // 트랜잭션(영속성 컨텍스트)이 없으면 선생님(호스트) 요청에서 LazyInitializationException이 나
    // catch에 삼켜져 리액션이 드롭됐다(학생은 그 분기를 안 타서 정상). 읽기 트랜잭션으로 감싸 지연로딩을 보장.
    @Transactional(readOnly = true)
    @MessageMapping("/classroom-sessions/{sessionId}/reactions")
    public void relay(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        if (principal == null) return;
        Long userId = Long.valueOf(principal.getName());

        // 수업 멤버(담당 선생님·ACTIVE 수강생)만 — 비멤버/존재하지 않는 세션이면 무시(브로드캐스트 X).
        ClassroomSession session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null) return;
        try {
            classroomService.verifyMemberAndIsHost(session.getCourse(), userId);
        } catch (Exception e) {
            log.debug("리액션 거부(비멤버): sessionId={}, userId={}", sessionId, userId);
            return;
        }

        message.put("senderId", userId);
        userRepository.findById(userId).ifPresent(u -> message.put("senderName", u.getName()));
        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/reactions",
                message
        );
    }
}
