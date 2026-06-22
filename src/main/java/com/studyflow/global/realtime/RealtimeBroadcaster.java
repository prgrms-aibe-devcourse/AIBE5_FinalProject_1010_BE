package com.studyflow.global.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 멀티 인스턴스(ALB 뒤 EC2 2대 이상) 환경에서 WebSocket 브로드캐스트를 인스턴스 간에 전달하는 릴레이.
 *
 * <p>문제: Spring 기본 simple broker(enableSimpleBroker)는 {@link SimpMessagingTemplate#convertAndSend}가
 * "그 JVM에 붙은 구독자"에게만 메시지를 보낸다. 인스턴스 A에서 보낸 화이트보드/오디오/채팅 이벤트가
 * 인스턴스 B에 붙은 참가자에게 전달되지 않아 동기화가 깨진다.</p>
 *
 * <p>해결: 브로드캐스트를 직접 로컬 broker로 보내지 않고, 먼저 Redis 채널(ws:broadcast)에 발행한다.
 * 모든 인스턴스가 이 채널을 구독({@link RealtimeRelayConfig})하다가, 메시지를 받으면 각자
 * 로컬 broker로 전달한다. 결과적으로 어느 인스턴스에서 보내든 전 인스턴스의 구독자에게 도달한다.
 * (발행 인스턴스 자신도 구독자로서 동일 경로로 받으므로, 기존 convertAndSend를 이 메서드로 바꾸기만 하면 된다.)</p>
 *
 * <p>로컬 전달은 {@link RealtimeRelayConfig}의 리스너가 {@link #deliverLocal}을 호출해 수행하며,
 * deliverLocal은 로컬 broker로만 보내므로(재발행 없음) 루프가 생기지 않는다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RealtimeBroadcaster {

    public static final String CHANNEL = "ws:broadcast";

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /** 토픽 브로드캐스트(/sub/...). 전 인스턴스의 해당 토픽 구독자에게 전달. */
    public void send(String destination, Object payload) {
        publish(new Envelope("topic", destination, null, payload));
    }

    /** 사용자 큐(/user/sub/...). 해당 사용자의 세션을 들고 있는 인스턴스에서만 실제 전달된다. */
    public void sendToUser(String user, String destination, Object payload) {
        publish(new Envelope("user", destination, user, payload));
    }

    private void publish(Envelope envelope) {
        try {
            redisTemplate.convertAndSend(CHANNEL, objectMapper.writeValueAsString(envelope));
        } catch (Exception e) {
            log.warn("[realtime] 브로드캐스트 발행 실패: dest={}", envelope.destination(), e);
        }
    }

    /** Redis 채널 리스너가 호출 — 이 인스턴스의 로컬 broker로만 전달(재발행 없음). */
    void deliverLocal(Envelope envelope) {
        if ("user".equals(envelope.type()) && envelope.user() != null) {
            messagingTemplate.convertAndSendToUser(envelope.user(), envelope.destination(), envelope.payload());
        } else {
            messagingTemplate.convertAndSend(envelope.destination(), envelope.payload());
        }
    }

    /** Redis로 직렬화돼 인스턴스 간 오가는 봉투. payload는 JSON 라운드트립 후 Map으로 복원돼도 클라 전송 결과는 동일. */
    public record Envelope(String type, String destination, String user, Object payload) {}
}
