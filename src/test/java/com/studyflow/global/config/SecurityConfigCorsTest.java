package com.studyflow.global.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link SecurityConfig#corsConfigurationSource()}의 오리진 허용 동작 검증.
 *
 * <p>PR 리뷰에서 "{@code http://192.168.*.*:5173} 같은 와일드카드 패턴이
 * {@code setAllowedOriginPatterns}에서 의도대로 동작하지 않을 수 있다"는 지적이 있어,
 * 실제 매칭 동작을 테스트로 고정한다. (Spring의 OriginPattern은 {@code *}를 정규식
 * {@code .*}로 변환하므로 IP 세그먼트 매칭에 사용할 수 있다)</p>
 */
class SecurityConfigCorsTest {

    /** cors.allowed-origins 프로퍼티가 비어 있을 때의 기본(개발) 설정을 만든다. */
    private CorsConfiguration corsConfigWithProp(String prop) {
        // corsConfigurationSource()는 다른 협력자를 쓰지 않으므로 나머지 의존성은 null로 둔다.
        SecurityConfig config = new SecurityConfig(null, null, null, null, null, null, null, null);
        ReflectionTestUtils.setField(config, "allowedOriginsProp", prop);

        CorsConfigurationSource source = config.corsConfigurationSource();
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/v1/auth/login");
        return source.getCorsConfiguration(request);
    }

    @Test
    @DisplayName("기본(프로퍼티 없음): localhost와 사설 IP 대역(192.168/10/172)의 5173 오리진을 허용한다")
    void defaultPatterns_allowLocalAndLanOrigins() {
        CorsConfiguration cors = corsConfigWithProp("");

        assertThat(cors.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
        assertThat(cors.checkOrigin("http://127.0.0.1:5173")).isEqualTo("http://127.0.0.1:5173");
        // LAN 와일드카드 패턴이 실제 IP에 매칭되는지 (리뷰 지적 검증 포인트)
        assertThat(cors.checkOrigin("http://192.168.0.23:5173")).isEqualTo("http://192.168.0.23:5173");
        assertThat(cors.checkOrigin("http://10.0.0.7:5173")).isEqualTo("http://10.0.0.7:5173");
        assertThat(cors.checkOrigin("http://172.16.1.2:5173")).isEqualTo("http://172.16.1.2:5173");
    }

    @Test
    @DisplayName("기본(프로퍼티 없음): 외부 도메인·다른 포트는 허용하지 않는다")
    void defaultPatterns_rejectForeignOrigins() {
        CorsConfiguration cors = corsConfigWithProp("");

        assertThat(cors.checkOrigin("http://evil.example.com:5173")).isNull();
        assertThat(cors.checkOrigin("http://192.168.0.23:3000")).isNull();
        assertThat(cors.checkOrigin("https://studyflow.example.com")).isNull();
    }

    @Test
    @DisplayName("프로퍼티 지정 시: 지정한 오리진만 정확히 허용한다(운영 오버라이드)")
    void propertyOverride_allowsOnlyListedOrigins() {
        CorsConfiguration cors = corsConfigWithProp("https://studyflow.example.com, https://admin.studyflow.example.com");

        assertThat(cors.checkOrigin("https://studyflow.example.com")).isEqualTo("https://studyflow.example.com");
        assertThat(cors.checkOrigin("http://192.168.0.23:5173")).isNull(); // LAN 기본 패턴은 비활성화됨
    }
}
