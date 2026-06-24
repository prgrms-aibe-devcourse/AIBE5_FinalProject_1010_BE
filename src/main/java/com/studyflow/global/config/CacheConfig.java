package com.studyflow.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String ADMIN_USER_COUNT         = "adminUserCount";
    public static final String ADMIN_INACTIVE_USER_COUNT = "adminInactiveUserCount";
    public static final String ADMIN_DELETED_USER_COUNT  = "adminDeletedUserCount";
    /** 메인 홈 "이번주 HOT 선생님" — 비로그인 공개 엔드포인트이므로 매 요청마다 집계 쿼리 방지. */
    public static final String HOT_TEACHERS = "hotTeachers";

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // 어드민 대시보드 카운트 캐시 — 5분 TTL
        RedisCacheConfiguration dashboardCountConfig = defaultConfig.entryTtl(Duration.ofMinutes(5));
        // 메인 홈 HOT 선생님 집계 캐시 — 10분 TTL (비로그인 공개 엔드포인트, 트래픽 집중 완화)
        RedisCacheConfiguration hotTeachersConfig = defaultConfig.entryTtl(Duration.ofMinutes(10));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        ADMIN_USER_COUNT,          dashboardCountConfig,
                        ADMIN_INACTIVE_USER_COUNT, dashboardCountConfig,
                        ADMIN_DELETED_USER_COUNT,  dashboardCountConfig,
                        HOT_TEACHERS,              hotTeachersConfig
                ))
                .build();
    }
}
