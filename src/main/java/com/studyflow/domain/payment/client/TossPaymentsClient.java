package com.studyflow.domain.payment.client;

import com.studyflow.domain.payment.config.TossPaymentsProperties;
import com.studyflow.domain.payment.exception.PaymentException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 결제 승인 클라이언트.
 *
 * <p>★ 승인은 반드시 서버에서 시크릿 키로 호출한다(프론트만으론 금액 위변조 가능).
 * 토스 승인 API는 secretKey + ":" 를 Base64로 인코딩해 Basic 인증으로 호출한다.</p>
 */
@Slf4j
@Component
public class TossPaymentsClient {

    private final RestClient restClient;
    private final TossPaymentsProperties props;
    private final String authHeader;

    public TossPaymentsClient(TossPaymentsProperties props) {
        this.props = props;
        this.restClient = RestClient.create();
        // Basic 인증: base64("{secretKey}:")  — 비밀번호 없이 콜론만 붙인다(토스 규격).
        String raw = (props.secretKey() == null ? "" : props.secretKey()) + ":";
        this.authHeader = "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 결제 승인. 성공하면 토스 결제 응답(Map)을 반환하고, 실패하면 PaymentException.
     *
     * @param paymentKey 토스 결제 키(프론트가 성공 콜백에서 받은 값)
     * @param orderId    우리 주문번호
     * @param amount     결제 금액(우리 주문 금액과 일치해야 함 — 호출 전 검증)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirm(String paymentKey, String orderId, long amount) {
        try {
            return restClient.post()
                    .uri(props.confirmUrl())
                    .header(HttpHeaders.AUTHORIZATION, authHeader)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderId,
                            "amount", amount
                    ))
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            // 4xx(승인 거절/금액 불일치/만료 등) 포함 모든 실패
            log.error("토스 결제 승인 실패 (orderId={})", orderId, e);
            throw new PaymentException("결제 승인에 실패했습니다. " + e.getMessage());
        }
    }
}
