package com.studyflow.global.redis;

public class RedisPrefixProvider {
    private static final String REFRESH_TOKEN_PREFIX = "rt:";
    private static final String TEST_PREFIX = "test:";
    private static final String SOCIAL_PENDING_PREFIX = "social:pending:";

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

    private static final String OAUTH2_CODE_PREFIX = "oauth2:code:";

    /** 소셜 로그인 성공 후 one-time code로 토큰을 교환할 때 사용하는 키 (TTL 30초) */
    public static String oauth2CodeKey(String code) {
        return OAUTH2_CODE_PREFIX + code;
    }
}
