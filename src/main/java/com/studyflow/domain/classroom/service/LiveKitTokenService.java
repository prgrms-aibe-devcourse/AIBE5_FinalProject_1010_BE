package com.studyflow.domain.classroom.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * LiveKit 접근 토큰(JWT) 발급 서비스.
 *
 * <p>LiveKit 토큰은 api-secret으로 HS256 서명한 JWT다(별도 SDK 불필요 — 기존 jjwt 재사용).
 * 클레임 구조:
 * <pre>
 *   iss = apiKey, sub = identity, name = 표시명, exp/nbf,
 *   video = { room, roomJoin:true, canPublish, canSubscribe:true, canPublishData:true }
 * </pre>
 * 토큰은 저장하지 않고 입장 시점마다 즉석 발급한다(단기 만료).</p>
 */
@Slf4j
@Service
public class LiveKitTokenService {

    private final String apiKey;
    private final String apiSecret;
    private final String url;

    // 토큰 유효시간(수업 1회 기준 충분) — 6시간
    private static final long TOKEN_TTL_MS = 6 * 60 * 60 * 1000L;

    public LiveKitTokenService(
            @Value("${livekit.api-key:}") String apiKey,
            @Value("${livekit.api-secret:}") String apiSecret,
            @Value("${livekit.url:}") String url
    ) {
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    /**
     * LiveKit 입장 토큰 발급.
     *
     * @param roomName    방 이름 (course-{courseId}-session-{sessionId})
     * @param identity    참가자 고유 식별자 (user-{userId})
     * @param displayName 표시 이름
     * @param canPublish  미디어 송출 권한(송출 게이팅) — 선생님/발표학생만 true
     */
    public String createToken(String roomName, String identity, String displayName, boolean canPublish) {
        SecretKey key = buildKey();

        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", roomName);
        videoGrant.put("roomJoin", true);
        videoGrant.put("canPublish", canPublish);
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", true); // 채팅/데이터 채널은 모두 허용(시청자도 상호작용 가능)

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(apiKey)
                .subject(identity)
                .claim("name", displayName)
                .claim("video", videoGrant)
                .issuedAt(new Date(now))
                .expiration(new Date(now + TOKEN_TTL_MS))
                .signWith(key) // 키 길이에 따라 HS256 (LiveKit 호환)
                .compact();
    }

    private SecretKey buildKey() {
        if (apiSecret == null || apiSecret.isBlank() || apiSecret.startsWith("your-")) {
            // 아직 실제 키 미설정(placeholder) — 명확한 메시지로 알림
            throw new IllegalStateException("LiveKit api-secret이 설정되지 않았습니다. application-secret.yml에 실제 키를 넣어주세요.");
        }
        try {
            return Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException e) {
            throw new IllegalStateException("LiveKit api-secret이 너무 짧습니다(HS256은 32바이트 이상 필요).", e);
        }
    }
}
