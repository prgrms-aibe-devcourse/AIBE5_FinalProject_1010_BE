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

    // 최초 발급 시 1회 생성 후 캐싱 (요청마다 키 재생성 방지)
    private volatile SecretKey cachedKey;

    // 토큰 유효시간(수업 1회 기준) — 2시간.
    // 종료(closeSession) 시 LiveKit 방을 실제로 닫지는 않으므로(서버 API 미연동), 발급된 토큰이
    // 오래 살아있지 않도록 TTL을 짧게 유지하는 완화책. (TODO: 종료 시 RoomService.DeleteRoom 호출)
    private static final long TOKEN_TTL_MS = 2 * 60 * 60 * 1000L;

    // 비수강생 미리보기 토큰 유효시간 — 60초. 짧은 TTL이 서버 측 자동 종료의 1차 방어선.
    private static final long PREVIEW_TOKEN_TTL_MS = 60 * 1000L;

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
                // 알고리즘을 HS256으로 명시 고정. signWith(key)만 쓰면 jjwt가 키 길이에 따라
                // HS384/HS512를 자동 선택하는데, LiveKit은 HS256 토큰만 검증하므로 긴 시크릿에서 전부 거부됨.
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 비수강생 미리보기 토큰 발급 — 보기 전용(canPublish=false), 데이터 채널 금지(canPublishData=false), TTL 60초.
     * 멤버용 createToken과 같은 룸에 입장하지만 송출·채팅·화이트보드 데이터 전송이 불가능하다.
     *
     * @param roomName    방 이름 (course-{courseId}-session-{sessionId}) — 멤버와 동일 룸
     * @param identity    미리보기 참가자 식별자 (preview-user-{userId})
     * @param displayName 표시 이름
     */
    public String createPreviewToken(String roomName, String identity, String displayName) {
        SecretKey key = buildKey();

        Map<String, Object> videoGrant = new HashMap<>();
        videoGrant.put("room", roomName);
        videoGrant.put("roomJoin", true);
        videoGrant.put("canPublish", false);    // 보기 전용 — 송출 불가
        videoGrant.put("canSubscribe", true);
        videoGrant.put("canPublishData", false); // 채팅/화이트보드 데이터 전송 금지

        long now = System.currentTimeMillis();
        return Jwts.builder()
                .issuer(apiKey)
                .subject(identity)
                .claim("name", displayName)
                .claim("video", videoGrant)
                .issuedAt(new Date(now))
                .expiration(new Date(now + PREVIEW_TOKEN_TTL_MS))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * HS256 서명 키를 반환(최초 1회 생성 후 캐싱).
     *
     * <p>생성자/@PostConstruct에서 미리 검증하지 않는 이유: 키 미설정(placeholder)이 기본 상태이며,
     * 그 경우에도 앱은 정상 기동되어야 한다(토큰 발급을 호출할 때만 실패하면 됨). 그래서 지연 생성한다.</p>
     */
    private SecretKey buildKey() {
        SecretKey local = cachedKey;
        if (local != null) return local;

        if (apiSecret == null || apiSecret.isBlank() || apiSecret.startsWith("your-")) {
            // 아직 실제 키 미설정(placeholder) — 명확한 메시지로 알림
            throw new IllegalStateException("LiveKit api-secret이 설정되지 않았습니다. application-secret.yml에 실제 키를 넣어주세요.");
        }
        try {
            local = Keys.hmacShaKeyFor(apiSecret.getBytes(StandardCharsets.UTF_8));
            cachedKey = local;
            return local;
        } catch (WeakKeyException e) {
            throw new IllegalStateException("LiveKit api-secret이 너무 짧습니다(HS256은 32바이트 이상 필요).", e);
        }
    }
}
