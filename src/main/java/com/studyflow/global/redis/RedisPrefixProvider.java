package com.studyflow.global.redis;

public class RedisPrefixProvider {
    private static final String REFRESH_TOKEN_PREFIX = "rt:";
    private static final String TEST_PREFIX = "test:";
    private static final String SOCIAL_PENDING_PREFIX = "social:pending:";
    private static final String EMAIL_AUTH_CODE_PREFIX = "email:auth:";
    private static final String EMAIL_VERIFIED_TOKEN_PREFIX = "email:verified:";

    // 인스턴스화 방지
    private RedisPrefixProvider() {}

    public static String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    public static String testKey(String suffix) {
        return TEST_PREFIX + suffix;
    }

    /** 소셜 로그인 추가 정보 입력 대기 중인 임시 데이터 키 */
    public static String socialPendingKey(String tempToken) {
        return SOCIAL_PENDING_PREFIX + tempToken;
    }

    /** 이메일 인증 코드 키 (TTL 5분) */
    public static String emailAuthCodeKey(String email) {
        return EMAIL_AUTH_CODE_PREFIX + email;
    }

    /** 이메일 인증 완료 후 발급되는 단회용 토큰 키 (TTL 10분) */
    public static String emailVerifiedTokenKey(String token) {
        return EMAIL_VERIFIED_TOKEN_PREFIX + token;
    }

    private static final String EMAIL_SEND_COOLDOWN_PREFIX = "email:cooldown:";
    private static final String EMAIL_SEND_COUNT_PREFIX = "email:sendcount:";
    private static final String EMAIL_VERIFY_ATTEMPT_PREFIX = "email:attempt:";

    /** 이메일 발송 쿨다운 키 (TTL = 쿨다운 시간) */
    public static String emailSendCooldownKey(String email) {
        return EMAIL_SEND_COOLDOWN_PREFIX + email;
    }

    /** 시간당 이메일 발송 횟수 카운터 키 (TTL 1시간) */
    public static String emailSendCountKey(String email) {
        return EMAIL_SEND_COUNT_PREFIX + email;
    }

    /** 이메일 인증 코드 검증 실패 횟수 카운터 키 */
    public static String emailVerifyAttemptKey(String email) {
        return EMAIL_VERIFY_ATTEMPT_PREFIX + email;
    }

    private static final String OAUTH2_CODE_PREFIX = "oauth2:code:";

    /** 소셜 로그인 성공 후 one-time code로 토큰을 교환할 때 사용하는 키 (TTL 30초) */
    public static String oauth2CodeKey(String code) {
        return OAUTH2_CODE_PREFIX + code;
    }
}
