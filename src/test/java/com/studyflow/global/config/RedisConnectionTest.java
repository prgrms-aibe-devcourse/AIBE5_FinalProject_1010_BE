package com.studyflow.global.config;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void redisConnectionTest() {
        // Given
        ValueOperations<String, String> valueOperations = redisTemplate.opsForValue();
        String key = "test_connect";
        String value = "success";

        // When (실제 Redis에 데이터를 넣을 때 인증을 진행합니다)
        valueOperations.set(key, value);

        // Then
        String result = valueOperations.get(key);
        Assertions.assertThat(result).isEqualTo("success");
    }
}
