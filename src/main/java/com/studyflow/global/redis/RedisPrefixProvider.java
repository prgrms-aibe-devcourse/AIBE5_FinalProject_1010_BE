package com.studyflow.global.redis;

public class RedisPrefixProvider {
    private static final String REFRESH_TOKEN_PREFIX = "rt:";
    private static final String TEST_PREFIX = "test:";

    // 인스턴스화 방지
    private RedisPrefixProvider() {}

    public static String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }

    public static String testKey(String suffix) {
        return TEST_PREFIX + suffix;
    }
}
