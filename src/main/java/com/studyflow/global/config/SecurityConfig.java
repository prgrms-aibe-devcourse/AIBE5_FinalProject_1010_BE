package com.studyflow.global.config;

import com.studyflow.domain.auth.handler.OAuth2FailureHandler;
import com.studyflow.domain.auth.handler.OAuth2SuccessHandler;
import com.studyflow.domain.auth.service.OAuth2UserService;
import com.studyflow.global.auth.JwtAccessDeniedHandler;
import com.studyflow.global.auth.JwtAuthenticationEntryPoint;
import com.studyflow.global.auth.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final PublicUrlProvider publicUrlProvider;
    private final OAuth2UserService oAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;
    private final OAuth2FailureHandler oAuth2FailureHandler;

    // Read allowed origins as a single comma-separated property (fallback to empty)
    @Value("${cors.allowed-origins:}")
    private String allowedOriginsProp;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(publicUrlProvider.getPublicUrls()).permitAll()
                        .requestMatchers(publicUrlProvider.getUrlsWithoutAccessToken()).permitAll()
                        .requestMatchers("/error").permitAll()
                        // 수업 등록 / 수정 / 삭제 — 선생님 전용 (optional-auth permitAll보다 먼저 선언해야 가려지지 않음)
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/courses/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*").hasRole("TEACHER")
                        // 수강 신청 — 학생 전용
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/enrollment-requests").hasRole("STUDENT")
                        // 수업 상세 GET — 비로그인 허용, GET만 permitAll (POST/PATCH/DELETE는 위에서 이미 처리)
                        .requestMatchers(HttpMethod.GET, "/api/v1/courses/*").permitAll()
                        .requestMatchers("/api/v1/auth/test/student").hasRole("STUDENT") // 테스트용
                        .requestMatchers("/api/v1/auth/test/teacher").hasRole("TEACHER") // 테스트용
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 처리
                        .accessDeniedHandler(jwtAccessDeniedHandler)) // 403 처리
                .oauth2Login(oauth2 -> oauth2
                        // Stateless 환경에서도 OAuth2 인가 요청 정보를 세션에 임시 저장
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestRepository(new HttpSessionOAuth2AuthorizationRequestRepository()))
                        .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService))
                        .successHandler(oAuth2SuccessHandler)
                        .failureHandler(oAuth2FailureHandler));
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 정책 설정
     *
     * 현재 설정:
     * - allowedOrigins: http://localhost:5173 (개발환경의 프론트엔드 예시)
     * - allowedMethods: GET, POST, PUT, DELETE, PATCH
     * - allowedHeaders: 모든 헤더 허용
     * - allowCredentials: true (쿠키/인증정보 전송 허용)
     *
     * 보안상 주의사항:
     * - allowCredentials(true)와 함께 allowedOrigins에 "*"(와일드카드)를 사용하는 것은 브라우저에서 허용되지 않습니다.
     *   즉, 자격증명 허용 시 특정 출처만 허용하도록 명시해야 합니다.
     * - production에서는 정확한 도메인 혹은 도메인 목록만 허용하세요. 개발 전용 출처는 배포 시 제거 또는 환경변수로 관리하세요.
     * - allowedHeaders를 "*"로 설정하면 편리하지만, 필요한 경우 최소한의 헤더만 허용하도록 제한하면 보안이 향상됩니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경 예시: 프론트엔드(예: Vite)의 로컬 호스트 허용
        List<String> origins;
        if (allowedOriginsProp == null || allowedOriginsProp.isBlank()) {
            origins = List.of("http://localhost:5173");
        } else {
            // support comma-separated or single value
            origins = java.util.Arrays.stream(allowedOriginsProp.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
        }
        configuration.setAllowedOrigins(origins);
        // 허용할 HTTP 메서드 (preflight를 위해 OPTIONS, HEAD 포함)
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        // 허용할 헤더 (필요 시 구체적으로 제한 권장)
        configuration.setAllowedHeaders(List.of("*"));
        // 쿠키/자격증명 전송 허용
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
