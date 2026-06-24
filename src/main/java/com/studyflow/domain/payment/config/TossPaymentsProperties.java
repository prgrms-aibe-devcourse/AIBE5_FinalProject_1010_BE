package com.studyflow.domain.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 토스페이먼츠 설정. {@code toss.*} 프로퍼티로 주입.
 *
 * <p>client-key는 프론트(결제창)용, secret-key는 백엔드 승인 API용(절대 노출 금지).
 * 둘은 같은 상점(테스트/실)에서 발급한 한 쌍이어야 한다.</p>
 *
 * @param clientKey 토스 클라이언트 키(프론트 SDK)
 * @param secretKey 토스 시크릿 키(백엔드 승인, Basic 인증)
 * @param confirmUrl 결제 승인 API URL
 */
@ConfigurationProperties(prefix = "toss")
public record TossPaymentsProperties(
        String clientKey,
        String secretKey,
        String confirmUrl
) {
    public TossPaymentsProperties {
        if (confirmUrl == null || confirmUrl.isBlank()) {
            confirmUrl = "https://api.tosspayments.com/v1/payments/confirm";
        }
    }
}
