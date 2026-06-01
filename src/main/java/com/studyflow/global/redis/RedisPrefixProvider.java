package com.studyflow.global.redis;

public class RedisPrefixProvider {
    private static final String REFRESH_TOKEN_PREFIX = "rt:";

    public static String refreshTokenKey(Long userId) {
        return REFRESH_TOKEN_PREFIX + userId;
    }
}
