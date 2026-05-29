package com.studyflow.global.websocket;

import java.security.Principal;

/**
 * STOMP WebSocket에서 사용할 인증 사용자 정보.
 *
 * Spring MVC의 @AuthenticationPrincipal과 달리 WebSocket 메시지에서는 Principal을 통해
 * 현재 사용자를 구분한다. name에는 userId를 문자열로 저장한다.
 */
public class StompPrincipal implements Principal {

    private final String name;

    public StompPrincipal(Long userId) {
        this.name = String.valueOf(userId);
    }

    @Override
    public String getName() {
        return name;
    }
}
