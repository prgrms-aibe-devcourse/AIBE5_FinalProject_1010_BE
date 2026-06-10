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

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        ADMIN_USER_COUNT,          dashboardCountConfig,
                        ADMIN_INACTIVE_USER_COUNT, dashboardCountConfig,
                        ADMIN_DELETED_USER_COUNT,  dashboardCountConfig
                ))
                .build();
    }
}
