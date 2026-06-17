package com.studyflow.domain.auth.controller;

import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.global.redis.RedisPrefixProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 이메일로 10명이 동시에 회원가입 요청을 보낼 때,
 * 정확히 1명만 201로 성공하고 나머지 9명은 409로 실패하는지 검증합니다.
 *
 * 실제 플로우에서 verifiedToken은 이메일당 1개만 존재할 수 있으므로,
 * 10개 스레드가 동일한 토큰 1개를 공유하여 경쟁하는 시나리오를 재현합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthSignupConcurrencyTest {

    private static final int THREAD_COUNT = 10;
    private static final String TEST_EMAIL = "concurrent-test@studyflow.com";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private StudentProfileRepository studentProfileRepository;

    @Autowired
    private TeacherProfileRepository teacherProfileRepository;

    // 외부 의존성 Mock — 실제 메일 발송 및 소셜 로그인 설정 불필요
    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    private String sharedToken;

    @BeforeEach
    void setUp() {
        // 실제 플로우와 동일하게 이메일당 verifiedToken 1개만 Redis에 저장합니다.
        sharedToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                RedisPrefixProvider.emailVerifiedTokenKey(sharedToken),
                TEST_EMAIL,
                Duration.ofMinutes(10)
        );
    }

    @AfterEach
    void tearDown() {
        // Redis에 남아있는 테스트용 토큰 정리 (성공 스레드가 삭제했을 수도 있음)
        redisTemplate.delete(RedisPrefixProvider.emailVerifiedTokenKey(sharedToken));
        // FK 참조 순서: StudentProfile / TeacherProfile 먼저 삭제 후 User 삭제
        userRepository.findActiveByEmailAndSocialProvider(TEST_EMAIL, SocialProvider.LOCAL)
        .ifPresent(user -> {
            studentProfileRepository.findByUserId(user.getId()).ifPresent(studentProfileRepository::delete);
            teacherProfileRepository.findByUserId(user.getId()).ifPresent(teacherProfileRepository::delete);
            userRepository.delete(user);
        });
    }

    @Test
    @DisplayName("동일 이메일 동시 회원가입 — 1명만 성공(201), 나머지 9명은 이메일 중복(409)")
    void concurrentSignup_onlyOneSucceeds() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);  // 모든 스레드를 동시에 출발시킵니다
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        List<Future<ResponseEntity<String>>> futures = new ArrayList<>();

        // 10개 스레드 모두 동일한 sharedToken으로 요청
        String requestBody = buildSignupRequestBody(TEST_EMAIL, sharedToken);

        for (int i = 0; i < THREAD_COUNT; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await(); // 출발 신호 대기
                try {
                    return restTemplate.postForEntity(
                            "/api/v1/auth/signup",
                            buildHttpEntity(requestBody),
                            String.class
                    );
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown(); // 모든 스레드 동시 출발
        doneLatch.await();      // 모든 요청 완료 대기
        executor.shutdown();

        // 결과 집계
        int successCount = 0;
        int failCount = 0;

        for (Future<ResponseEntity<String>> future : futures) {
            ResponseEntity<String> response;
            try {
                response = future.get();
            } catch (Exception e) {
                throw new RuntimeException("요청 실행 중 예외 발생", e);
            }
            int status = response.getStatusCode().value();
            if (status == HttpStatus.CREATED.value()) {
                successCount++;
            } else if (status == HttpStatus.CONFLICT.value()       // 이메일 중복 감지
                    || status == HttpStatus.BAD_REQUEST.value()) {  // 토큰이 이미 삭제된 경우
                failCount++;
            }
        }

        assertThat(successCount)
                .as("정확히 1명만 회원가입에 성공해야 합니다")
                .isEqualTo(1);

        assertThat(failCount)
                .as("나머지 9명은 이메일 중복(409) 또는 토큰 만료(400)를 받아야 합니다")
                .isEqualTo(THREAD_COUNT - 1);

        // DB에 해당 이메일로 가입된 계정이 정확히 1개인지 확인
        assertThat(userRepository.findActiveByEmailAndSocialProvider(TEST_EMAIL, SocialProvider.LOCAL))
                .as("DB에 동일 이메일 계정은 정확히 1개여야 합니다")
                .isPresent();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    private String buildSignupRequestBody(String email, String verifiedToken) {
        return """
                {
                  "email": "%s",
                  "password": "Password123!",
                  "passwordConfirm": "Password123!",
                  "name": "동시성테스트",
                  "phone": "01012345678",
                  "role": "STUDENT",
                  "gender": "MALE",
                  "birthDate": "2000-01-01",
                  "verifiedToken": "%s",
                  "termsAgreements": [
                    { "termsType": "SERVICE",   "isAgreed": true  },
                    { "termsType": "PRIVACY",   "isAgreed": true  },
                    { "termsType": "MARKETING", "isAgreed": false }
                  ]
                }
                """.formatted(email, verifiedToken);
    }

    private HttpEntity<String> buildHttpEntity(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, headers);
    }
}
