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
    private final CookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

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
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── 역할 기반 규칙: permitAll보다 반드시 먼저 선언 ──
                        // Spring Security는 첫 번째 매칭 규칙을 적용하므로,
                        // publicUrls의 permitAll이 앞에 오면 같은 경로의 역할 규칙이 가려집니다.
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/courses/*/close").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/courses/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/courses/*").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/enrollment-requests").hasRole("STUDENT")
                        .requestMatchers("/api/v1/enrollment-requests/{requestId}/cancel").hasRole("STUDENT")
                        .requestMatchers("/api/v1/enrollment-requests/{requestId}/accept").hasRole("TEACHER")
                        .requestMatchers("/api/v1/enrollment-requests/{requestId}/reject").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/enrollments/*/drop").hasRole("STUDENT")
                        // QnA: 질문 작성/답변 채택은 STUDENT, 답변 작성은 TEACHER (GET은 optionalAuth로 공개)
                        .requestMatchers(HttpMethod.POST, "/api/v1/qna/questions").hasRole("STUDENT")
                        .requestMatchers(HttpMethod.POST, "/api/v1/qna/questions/*/answers").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/qna/answers/*/accept").hasRole("STUDENT")
                        // 질문 수정/삭제는 소유권 기반(역할 무관)이라 인증만 강제. (optionalAuth permitAll은 GET만 의도)
                        // 이 규칙이 없으면 questions/* permitAll 때문에 비인증 PATCH/DELETE가 통과되므로 반드시 필요.
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/qna/questions/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/qna/questions/*").authenticated()
                        // 답변 수정/삭제도 소유권 기반. 현재 answers/*는 optionalAuth에 없어 default(authenticated)로도
                        // 보호되지만, 추후 optionalAuth 추가 시 회귀를 막기 위해 명시한다.
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/qna/answers/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/qna/answers/*").authenticated()
                        // 강의실: 열기/종료/참가자 권한변경은 담당 선생님(TEACHER)만.
                        // 그 외(현재 조회 GET /courses/*/classroom-sessions/current, 참가 POST /classroom-sessions/*/participants,
                        // 토큰발급 POST /classroom-sessions/*/livekit-token)는 별도 역할 규칙 없이 기본 anyRequest().authenticated()에 의존하고,
                        // "수업 멤버(담당교사·ACTIVE 수강생)" 여부는 ClassroomService에서 검증한다. (의도된 설계)
                        .requestMatchers(HttpMethod.POST, "/api/v1/courses/*/classroom-sessions").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/classroom-sessions/*/close").hasRole("TEACHER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/classroom-participants/*/permissions").hasRole("TEACHER")
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // ── 공개 규칙: 역할 규칙 이후에 선언 ──
                        .requestMatchers(publicUrlProvider.getPublicUrls()).permitAll()
                        // getOptionalAuthUrls() 경로: GET만 JwtFilter에서 optional 처리 (JwtAuthenticationFilter 참고).
                        // POST/PATCH/DELETE 보호는 위 역할 기반 규칙이 담당하므로 이 permitAll()은 GET에만 유효.
                        .requestMatchers(publicUrlProvider.getOptionalAuthUrls()).permitAll()
                        .requestMatchers(publicUrlProvider.getUrlsWithoutAccessToken()).permitAll()
                        // 수업 검색(목록) · 수업 상세 GET — 비로그인 허용 - getOptionalAuthUrls()에 반영
                        .requestMatchers("/error").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) // 401 처리
                        .accessDeniedHandler(jwtAccessDeniedHandler)) // 403 처리
                .oauth2Login(oauth2 -> oauth2
                        // 세션 대신 쿠키에 저장 → 다중 서버 배포 시에도 stateless 유지
                        .authorizationEndpoint(auth -> auth
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository))
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
     * - allowCredentials(true)와 함께 allowedOrigins에 "*"(와일드카드)를 사용하는 것은 브라우저에서 허용되지
     * 않습니다.
     * 즉, 자격증명 허용 시 특정 출처만 허용하도록 명시해야 합니다.
     * - production에서는 정확한 도메인 혹은 도메인 목록만 허용하세요. 개발 전용 출처는 배포 시 제거 또는 환경변수로 관리하세요.
     * - allowedHeaders를 "*"로 설정하면 편리하지만, 필요한 경우 최소한의 헤더만 허용하도록 제한하면 보안이 향상됩니다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 개발 환경 예시: 로컬 및 같은 LAN에서 띄운 Vite 프론트엔드 허용
        List<String> origins;
        if (allowedOriginsProp == null || allowedOriginsProp.isBlank()) {
            origins = List.of(
                    "http://localhost:5173",
                    "http://127.0.0.1:5173",
                    "http://192.168.*.*:5173",
                    "http://10.*.*.*:5173",
                    "http://172.*.*.*:5173"
            );
            configuration.setAllowedOriginPatterns(origins);
        } else {
            // support comma-separated or single value
            origins = java.util.Arrays.stream(allowedOriginsProp.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .toList();
            configuration.setAllowedOrigins(origins);
        }
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
