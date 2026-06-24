package com.studyflow.global.auth;

import com.studyflow.global.exception.AuthException;
import com.studyflow.global.exception.ErrorCode;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Getter
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    public String createAccessToken(Long userId, String role) {
        return createToken(userId, role, accessTokenExpiration, "access");
    }

    public String createRefreshToken(Long userId, String role) {
        return createToken(userId, role, refreshTokenExpiration, "refresh");
    }

    private String createToken(Long userId, String role, long expiration, String tokenType) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .claim("type", tokenType)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(secretKey)
                .compact();
    }

    // 토큰 검증 관련 메서드

    // 주어진 토큰의 검증 성공 여부만 반환 (예외 발생 시 false)
    public boolean validateToken(String token) {
        try {
            validateTokenOrThrow(token);
            return true;
        } catch (AuthException ex) {
            // 이미 로그는 validateTokenOrThrow 안에서 남기므로 간단히 false를 반환
            return false;
        }
    }

    /**
     * 토큰을 검증하고, 문제 발생 시 관련된 {@link AuthException}을 던집니다.
     * 컨트롤러 등에서 에러 코드를 전달하려면 이 메서드를 호출하고 예외를 처리하면 됩니다.
     */
    public void validateTokenOrThrow(String token) {
        // reuse validateAndGetClaims to perform validation; discard returned claims
        validateAndGetClaims(token);
    }

    /**
     * 검증(헤더 alg 검사, 서명/만료 검사)과 함께 Claims(토큰 페이로드로 이해하면 편함)를 반환합니다.
     * 문제 발생 시 {@link AuthException}을 던집니다.
     */
    public Claims validateAndGetClaims(String token) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("JWT token is null or empty");
            throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "Token is null or empty");
        }

        // 헤더의 alg 값이 서버에서 기대하는 알고리즘(예: HS256)인지 먼저 확인합니다.
        try {
            String[] parts = token.split("\\.");
            if (parts.length < 2) {
                log.warn("Malformed JWT token (not enough parts)");
                throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "Malformed token");
            }
            String headerJson = new String(Decoders.BASE64URL.decode(parts[0]));
            Pattern p = Pattern.compile("\"alg\"\\s*:\\s*\"([^\"]+)\"");
            Matcher m = p.matcher(headerJson);
            if (m.find()) {
                String alg = m.group(1);
                if (!"HS256".equals(alg)) {
                    log.warn("Unexpected JWT alg: {}", alg);
                    throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "Unexpected alg: " + alg);
                }
            } else {
                log.warn("JWT header does not contain alg");
                throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "alg not present");
            }
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to parse JWT header: {}", e.getMessage());
            throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, "Failed to parse header");
        }

        try {
            return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Expired JWT token: {}", e.getMessage());
            throw new AuthException(ErrorCode.AUTH_EXPIRED_TOKEN, e.getMessage());
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            throw new AuthException(ErrorCode.AUTH_INVALID_TOKEN, e.getMessage());
        }
    }

    // 토큰을 검증한 후 Claims에서 userId를 추출
    public Long getUserId(String token) {
        Claims claims = validateAndGetClaims(token);
        return Long.parseLong(claims.getSubject());
    }

    // 토큰을 검증한 후 Claims에서 role을 추출
    public String getRole(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.get("role", String.class);
    }

    // 토큰을 검증한 후 Claims에서 type(access/refresh)을 추출
    public String getTokenType(String token) {
        Claims claims = validateAndGetClaims(token);
        return claims.get("type", String.class);
    }

    /**
     * Claims에서 userId를 추출합니다. 이미 Claims를 가지고 있을 때 사용하세요
     * (validateAndGetClaims를 한 번만 호출하고 여러번 재사용하기 위한 편의 메서드).
     */
    public Long getUserIdFromClaims(Claims claims) {
        if (claims == null) return null;
        return Long.parseLong(claims.getSubject());
    }

    /**
     * Claims에서 role을 추출합니다. 이미 Claims를 가지고 있을 때 사용하세요.
     */
    public String getRoleFromClaims(Claims claims) {
        if (claims == null) return null;
        return claims.get("role", String.class);
    }

    /**
     * Claims에서 토큰 type을 추출합니다. 이미 Claims를 가지고 있을 때 사용하세요.
     */
    public String getTypeFromClaims(Claims claims) {
        if (claims == null) return null;
        return claims.get("type", String.class);
    }
}
