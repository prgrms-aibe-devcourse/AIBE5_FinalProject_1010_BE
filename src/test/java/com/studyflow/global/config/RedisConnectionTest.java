package com.studyflow.global.config;

import com.studyflow.global.redis.RedisPrefixProvider;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String TEST_KEY = RedisPrefixProvider.testKey("connect");

    @AfterEach
    void tearDown() {
        redisTemplate.delete(TEST_KEY);
    }

    @Test
    void redisConnectionTest() {
        // Given
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String value = "success";

        // When (실제 Redis에 데이터를 넣을 때 인증을 진행합니다)
        valueOperations.set(TEST_KEY, value);

        // Then
        String result = valueOperations.get(TEST_KEY);
        Assertions.assertThat(result).isEqualTo("success");
    }
}
