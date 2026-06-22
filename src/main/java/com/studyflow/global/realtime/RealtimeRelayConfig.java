package com.studyflow.global.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * WS 브로드캐스트 Redis 릴레이 구독 설정 (멀티 인스턴스).
 * Redis 채널 {@link RealtimeBroadcaster#CHANNEL}을 구독하다가, 메시지가 오면 이 인스턴스의 로컬 broker로 전달한다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RealtimeRelayConfig {

    private final RealtimeBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer wsRelayListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            try {
                RealtimeBroadcaster.Envelope envelope =
                        objectMapper.readValue(message.getBody(), RealtimeBroadcaster.Envelope.class);
                broadcaster.deliverLocal(envelope);
            } catch (Exception e) {
                log.warn("[realtime] 브로드캐스트 수신 처리 실패", e);
            }
        }, new ChannelTopic(RealtimeBroadcaster.CHANNEL));
        return container;
    }
}
