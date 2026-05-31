package com.studyflow.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * OpenAI 호출 전용 RestClient 설정.
 *
 * <p>Spring Boot 3.5(Spring 6.2)에 내장된 {@link RestClient}를 사용하므로 별도 의존성이 필요 없다.
 * 공통 기본값(base-url, 인증 헤더, 타임아웃)을 여기서 한 번에 묶어 Bean으로 등록하고,
 * {@code @Qualifier("openAiRestClient")}로 주입해 쓴다.</p>
 *
 * <p>API 키는 보안상 소스/yml에 직접 적지 않고 환경변수(OPENAI_API_KEY)로 주입한다.</p>
 */
@Configuration
public class OpenAiConfig {

    @Bean
    public RestClient openAiRestClient(
            @Value("${openai.base-url}") String baseUrl,
            // 키가 정의되지 않아도(시크릿 파일 부재) 앱이 기동되도록 빈 문자열을 기본값으로 둔다.
            // 이 경우 실제 호출 시 OpenAI가 401을 반환하고 AiServiceException(502)으로 변환된다.
            @Value("${openai.api-key:}") String apiKey,
            @Value("${openai.timeout-seconds:60}") long timeoutSeconds
    ) {
        // OpenAI 응답은 수 초~수십 초가 걸릴 수 있으므로 read 타임아웃을 넉넉히 둔다.
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.defaults()
                .withConnectTimeout(Duration.ofSeconds(10))
                .withReadTimeout(Duration.ofSeconds(timeoutSeconds));
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactoryBuilder.detect().build(settings);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
