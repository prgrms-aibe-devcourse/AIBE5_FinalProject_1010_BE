package com.studyflow.domain.qna.service;

import com.studyflow.domain.qna.dto.request.QnaAnswerRequest;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.qna.repository.QnaQuestionRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.notification.repository.NotificationRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 학생의 질문 삭제(1) + 선생님들의 답변 작성(10)이 동시에 발생할 때
 * 데이터 정합성을 검증합니다.
 *
 * 비관적 락(findByIdWithLock)이 적용되어 있으므로,
 * 트랜잭션 진입 순서와 무관하게 최종 상태는 다음 중 하나여야 합니다.
 *
 *   A) 삭제가 먼저 락 획득 → question 삭제 커밋 → 이후 답변 시도들은
 *      QnaQuestionNotFoundException(404)으로 실패 → DB에 question·answer 없음
 *
 *   B) 일부 답변이 먼저 락 획득 → answer 저장 커밋 → 이후 삭제가 락 획득 →
 *      cascade ALL + orphanRemoval로 answer까지 모두 삭제 → DB에 question·answer 없음
 *
 * 어떤 순서가 되어도 최종 상태: question 없음 + answer 없음.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class QnaDeleteAnswerConcurrencyTest {

    private static final int TEACHER_COUNT = 10;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired private QnaService qnaService;
    @Autowired private QnaQuestionRepository questionRepository;
    @Autowired private QnaAnswerRepository answerRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long studentId;
    private List<Long> teacherIds;
    private Long questionId;
    private String testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 학생 생성
        User student = userRepository.save(
                User.createForTest("qna-student-" + testId + "@test.studyflow.com", "QnA테스트학생", UserRole.STUDENT));
        studentId = student.getId();

        // 선생님 10명 생성
        teacherIds = new ArrayList<>();
        for (int i = 1; i <= TEACHER_COUNT; i++) {
            User teacher = userRepository.save(
                    User.createForTest("qna-teacher" + i + "-" + testId + "@test.studyflow.com",
                            "QnA테스트선생님" + i, UserRole.TEACHER));
            teacherIds.add(teacher.getId());
        }

        // 질문 생성 (트랜잭션 내에서 저장)
        Subject subject = subjectRepository.findByParentSubjectIsNullOrderByIdAsc().get(0);
        questionId = transactionTemplate.execute(status -> {
            User author = userRepository.findById(studentId).orElseThrow();
            QnaQuestion question = QnaQuestion.create(author, subject, "동시성 테스트 질문", "테스트 내용입니다.");
            return questionRepository.save(question).getId();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            // 알림 정리 — 학생(질문 작성자)에게 전송된 답변 알림
            notificationRepository.deleteAllByRecipientId(studentId);

            // 질문이 아직 남아있다면(테스트 실패 케이스) 삭제 — cascade로 답변도 함께 삭제됨
            questionRepository.findById(questionId).ifPresent(questionRepository::delete);

            // 유저 정리
            teacherIds.forEach(id -> userRepository.findById(id).ifPresent(userRepository::delete));
            userRepository.findById(studentId).ifPresent(userRepository::delete);
        });
    }

    @Test
    @DisplayName("학생 질문 삭제(1) + 선생님 답변 작성(10) 동시 실행 — 최종적으로 질문과 답변이 모두 없어야 함")
    void concurrentDeleteAndAnswer_noOrphanedDataRemains() throws InterruptedException {
        int threadCount = 1 + TEACHER_COUNT; // 학생 1 + 선생님 10
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        AtomicBoolean deleteSucceeded = new AtomicBoolean(false);

        // 학생: 질문 삭제
        executor.submit(() -> {
            try {
                startLatch.await();
                qnaService.deleteQuestion(studentId, questionId, false);
                deleteSucceeded.set(true);
            } catch (Exception e) {
                // 삭제 실패는 이 테스트에서는 예상하지 않음
            } finally {
                doneLatch.countDown();
            }
        });

        // 선생님 10명: 동시에 답변 작성
        QnaAnswerRequest answerRequest = buildAnswerRequest();
        for (Long teacherId : teacherIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    qnaService.createAnswer(teacherId, questionId, answerRequest);
                } catch (Exception e) {
                    // QnaQuestionNotFoundException (삭제가 먼저 끝난 경우) — 정상 실패
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 11개 스레드 동시 출발
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished)
                .as("11개 스레드 모두 제한 시간(30초) 내에 완료되어야 합니다")
                .isTrue();

        assertThat(deleteSucceeded.get())
                .as("학생의 질문 삭제는 반드시 성공해야 합니다")
                .isTrue();

        // 핵심 검증: question도 없고 answer도 없어야 함
        assertThat(questionRepository.findById(questionId))
                .as("삭제된 질문이 DB에 남아있어서는 안 됩니다")
                .isEmpty();

        assertThat(answerRepository.countByQuestionId(questionId))
                .as("삭제된 질문의 답변이 DB에 고아 레코드로 남아있어서는 안 됩니다")
                .isZero();
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    /** QnaAnswerRequest — setter 없이 content만 세팅하는 테스트 전용 서브클래스. */
    private static class TestAnswerRequest extends QnaAnswerRequest {
        TestAnswerRequest(String content) {
            try {
                var field = QnaAnswerRequest.class.getDeclaredField("content");
                field.setAccessible(true);
                field.set(this, content);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private QnaAnswerRequest buildAnswerRequest() {
        return new TestAnswerRequest("동시성 테스트 답변입니다.");
    }
}
