package com.studyflow.global.websocket;

import java.security.Principal;

/**
 * STOMP WebSocket에서 사용할 인증 사용자 정보.
 *
 * Spring MVC의 @AuthenticationPrincipal과 달리 WebSocket 메시지에서는 Principal을 통해
 * 현재 사용자를 구분한다. name에는 userId를 문자열로 저장한다.
 *
 * <p>userId가 null이면 비로그인 "게스트"(실시간 강의실 미리보기 시청자)다.
 * 게스트는 화이트보드 구독만 허용되고 발행/다른 토픽 구독은 인터셉터에서 차단된다.</p>
 */
public class StompPrincipal implements Principal {

    private final Long userId; // null이면 게스트(비로그인 미리보기)

    public StompPrincipal(Long userId) {
        this.userId = userId;
    }

    @Override
    public String getName() {
        return userId == null ? "guest" : String.valueOf(userId);
    }

    /** 비로그인 게스트(미리보기 시청자) 여부. */
    public boolean isGuest() {
        return userId == null;
    }

    public Long getUserId() {
        return userId;
    }
}
