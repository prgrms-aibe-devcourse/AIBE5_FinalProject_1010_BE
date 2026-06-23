package com.studyflow.domain.payment.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 결제 모듈 설정. 토스 프로퍼티 바인딩 활성화.
 */
@Configuration
@EnableConfigurationProperties(TossPaymentsProperties.class)
public class PaymentConfig {
}
