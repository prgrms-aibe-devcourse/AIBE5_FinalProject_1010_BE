package com.studyflow.domain.classroom.controller;

import com.studyflow.domain.classroom.service.WhiteboardDrawPermissionStore;
import com.studyflow.domain.classroom.service.WhiteboardStateStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 강의실 화이트보드 실시간 동기화 WebSocket 컨트롤러 — 서버 권위(authoritative) 방식 (이슈 #131).
 *
 * <p>발행: /pub/classroom-sessions/{sessionId}/whiteboard — 구독: /sub/classroom-sessions/{sessionId}/whiteboard.
 * 서버가 단일 진실원천이다. 클라이언트가 보낸 변경 의도(ops)를 서버의 권위 상태에 반영하고,
 * 단조 증가하는 순번(seq)을 붙여 같은 세션 전원에게(보낸 사람 포함) 재방송한다.
 * 모든 클라이언트가 동일한 seq 순서로 동일한 ops를 적용 → 화면이 분기하지 않는다.</p>
 *
 * <p>메시지 종류:
 * <ul>
 *   <li>type == "ops": {ops:[...]} — 상태에 반영 후 seq를 붙여 재방송. 클라는 seq 순서대로 적용하고
 *       구멍이 나면 REST로 전체 상태를 다시 받아 자가 치유한다.</li>
 *   <li>type == "live": 그리는 중 미리보기 — 상태 변경/순번 없이 그대로 중계(휘발성).</li>
 * </ul>
 * 인증은 STOMP CONNECT 시 WebSocketAuthChannelInterceptor가 Principal에 userId를 넣어둔다.
 * op는 고빈도라 메시지마다 DB 멤버십 검증은 하지 않고 CONNECT 인증으로 1차 게이팅한다.
 * 추가로 "판서 권한"(이슈 #162)은 메모리 캐시({@link WhiteboardDrawPermissionStore})로 검사한다
 * — 기본적으로 선생님만 그릴 수 있고, 권한 없는 학생의 ops/live는 무시한다.</p>
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ClassroomWhiteboardWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final WhiteboardStateStore stateStore;
    private final WhiteboardDrawPermissionStore drawPermissionStore;

    @MessageMapping("/classroom-sessions/{sessionId}/whiteboard")
    public void relay(
            @DestinationVariable Long sessionId,
            @Payload Map<String, Object> message,
            Principal principal
    ) {
        // 인증된 연결만 처리(WebSocketAuthChannelInterceptor가 미인증을 막지만 방어적으로 한 번 더).
        if (principal == null) {
            return;
        }
        Long senderId = Long.valueOf(principal.getName());
        message.put("senderId", senderId);

        String type = String.valueOf(message.get("type"));

        // 판서 권한 게이팅(이슈 #162) — 권한 없는 참가자(기본: 학생)의 변경(ops)·미리보기(live)·페이지이동(page)은 조용히 무시.
        // 선생님(호스트)은 입장 시 canDraw=true. 권한자 집합은 인메모리 캐시로 검사(고빈도 메시지라 DB 미조회).
        if (("ops".equals(type) || "live".equals(type) || "page".equals(type)) && !drawPermissionStore.canDraw(sessionId, senderId)) {
            return;
        }

        // 페이지 이동: 전원이 같은 페이지를 보도록 활성 페이지를 갱신하고 그대로 중계(상태 op 아님, 순번 없음).
        if ("page".equals(type)) {
            stateStore.setActivePage(sessionId, String.valueOf(message.get("pageId")));
            broadcast(sessionId, message);
            return;
        }

        // 그리는 중 미리보기: 상태를 바꾸지 않는 휘발성 메시지. 순번 없이 그대로 중계.
        if ("live".equals(type)) {
            broadcast(sessionId, message);
            return;
        }

        // 확정 변경: 서버 권위 상태에 반영하고 순번(seq)을 붙여 전원에게 재방송.
        if ("ops".equals(type)) {
            List<Map<String, Object>> ops = castOps(message.get("ops"));
            if (ops == null || ops.isEmpty()) {
                return; // 변경 없는 메시지는 무시(상태/순번/브로드캐스트 모두 생략)
            }
            long seq = stateStore.apply(sessionId, ops);
            message.put("seq", seq);
            broadcast(sessionId, message);
            return;
        }
        // 그 외(과거 snapshot 등) 타입은 무시 — 서버가 이미 권위 상태를 들고 있다.
    }

    private void broadcast(Long sessionId, Map<String, Object> message) {
        messagingTemplate.convertAndSend(
                "/sub/classroom-sessions/" + sessionId + "/whiteboard",
                message
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castOps(Object ops) {
        return (ops instanceof List) ? (List<Map<String, Object>>) ops : null;
    }
}
