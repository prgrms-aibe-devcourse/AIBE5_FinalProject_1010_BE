package com.studyflow.domain.classroom.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * LiveKitTokenService 단위 테스트.
 *
 * <p>핵심: 서명 알고리즘이 키 길이와 무관하게 항상 HS256으로 고정되는지 검증한다.
 * (signWith(key)만 쓰면 64바이트 시크릿에서 jjwt가 HS512를 선택해 LiveKit이 토큰을 거부한다.)</p>
 */
class LiveKitTokenServiceTest {

    private static final String API_KEY = "APItestkey";
    // 64바이트(64자) 시크릿 — 옛 코드라면 HS512로 서명됐을 길이
    private static final String LONG_SECRET =
            "0123456789012345678901234567890123456789012345678901234567890123";
    private static final String URL = "wss://test.livekit.cloud";

    @Test
    @DisplayName("긴 시크릿(64바이트)에서도 토큰 alg 헤더는 HS256으로 고정된다")
    void token_alg_isAlwaysHs256() {
        LiveKitTokenService service = new LiveKitTokenService(API_KEY, LONG_SECRET, URL);

        String token = service.createToken("course-1-session-1", "user-1", "홍길동", true);

        String headerJson = new String(
                Base64.getUrlDecoder().decode(token.split("\\.")[0]), StandardCharsets.UTF_8);
        assertThat(headerJson).contains("\"alg\":\"HS256\"");
    }

    @Test
    @DisplayName("토큰 클레임 구조(iss/sub/name/video grant)가 LiveKit 규격과 일치한다")
    void token_claims_matchLivekitGrant() {
        LiveKitTokenService service = new LiveKitTokenService(API_KEY, LONG_SECRET, URL);

        String token = service.createToken("course-1-session-1", "user-7", "김학생", false);

        SecretKey key = Keys.hmacShaKeyFor(LONG_SECRET.getBytes(StandardCharsets.UTF_8));
        Claims claims = Jwts.parser().verifyWith(key).build()
                .parseSignedClaims(token).getPayload();

        assertThat(claims.getIssuer()).isEqualTo(API_KEY);
        assertThat(claims.getSubject()).isEqualTo("user-7");
        assertThat(claims.get("name")).isEqualTo("김학생");

        @SuppressWarnings("unchecked")
        Map<String, Object> video = claims.get("video", Map.class);
        assertThat(video.get("room")).isEqualTo("course-1-session-1");
        assertThat(video.get("roomJoin")).isEqualTo(true);
        assertThat(video.get("canPublish")).isEqualTo(false); // 송출 게이팅: 학생 기본 false
        assertThat(video.get("canSubscribe")).isEqualTo(true);
        assertThat(video.get("canPublishData")).isEqualTo(true);
    }

    @Test
    @DisplayName("api-secret이 placeholder면 토큰 발급 시 IllegalStateException")
    void token_placeholderSecret_throws() {
        LiveKitTokenService service = new LiveKitTokenService(API_KEY, "your-livekit-api-secret", URL);

        assertThatThrownBy(() -> service.createToken("room", "user-1", "이름", true))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("api-secret이 너무 짧으면(<32바이트) IllegalStateException")
    void token_shortSecret_throws() {
        LiveKitTokenService service = new LiveKitTokenService(API_KEY, "short-secret", URL);

        assertThatThrownBy(() -> service.createToken("room", "user-1", "이름", true))
                .isInstanceOf(IllegalStateException.class);
    }
}
