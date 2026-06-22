package com.studyflow.domain.naegong.service;

import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.qna.repository.QnaQuestionRepository;
import com.studyflow.domain.qna.service.QnaService;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
import com.studyflow.domain.notification.repository.NotificationRepository;
import com.studyflow.domain.naegong.repository.NaegongHistoryRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.user.repository.UserRepository;
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
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 한 선생님이 10개의 질문에 각각 답변을 작성한 뒤,
 * 10개의 답변이 모두 동시에 채택되었을 때 naegongScore가 정확히 100인지 검증합니다.
 *
 * 채택 시 naegongScore += 10 (ACCEPT_ANSWER_NAEGONG_SCORE).
 * 비관적 락(findByUserIdWithLock) 없이 read-modify-write를 수행하면
 * 10개 트랜잭션이 모두 초기값(0)을 읽고 각자 10을 더해 저장하므로 최종값이 10이 됩니다.
 * 락을 걸면 10개 트랜잭션이 직렬화되어 0→10→20→...→100 순서로 정확히 갱신됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class NaegongScoreConcurrencyTest {

    private static final int ANSWER_COUNT = 10;
    private static final int SCORE_PER_ACCEPT = 10;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired private QnaService qnaService;
    @Autowired private QnaQuestionRepository questionRepository;
    @Autowired private QnaAnswerRepository answerRepository;
    @Autowired private TeacherProfileRepository teacherProfileRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private NaegongHistoryRepository naegongHistoryRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long teacherId;
    private Long teacherProfileId;
    private Long studentId;
    private List<Long> questionIds;
    private List<Long> answerIds;
    private String testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 선생님 + 프로필 생성
        User teacher = userRepository.save(
                User.createForTest("naegong-teacher-" + testId + "@test.studyflow.com", "내공테스트선생님", UserRole.TEACHER));
        teacherId = teacher.getId();
        TeacherProfile tp = teacherProfileRepository.save(TeacherProfile.createForUser(teacher));
        teacherProfileId = tp.getId();

        // 학생 생성 (질문 작성자)
        User student = userRepository.save(
                User.createForTest("naegong-student-" + testId + "@test.studyflow.com", "내공테스트학생", UserRole.STUDENT));
        studentId = student.getId();

        Subject subject = subjectRepository.findByParentSubjectIsNullOrderByIdAsc().get(0);

        // 질문 10개 + 각 질문에 선생님 답변 1개씩 생성
        questionIds = new ArrayList<>();
        answerIds = new ArrayList<>();
        transactionTemplate.executeWithoutResult(status -> {
            User t = userRepository.findById(teacherId).orElseThrow();
            User s = userRepository.findById(studentId).orElseThrow();

            for (int i = 1; i <= ANSWER_COUNT; i++) {
                QnaQuestion question = questionRepository.save(
                        QnaQuestion.create(s, subject, "내공 동시성 테스트 질문 " + i, "테스트 내용 " + i));
                questionIds.add(question.getId());

                QnaAnswer answer = answerRepository.save(
                        QnaAnswer.create(question, t, "테스트 답변 " + i));
                answerIds.add(answer.getId());
            }
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            // 알림 정리
            notificationRepository.deleteAllByRecipientId(teacherId);
            notificationRepository.deleteAllByRecipientId(studentId);

            // 내공 이력 정리
            naegongHistoryRepository.deleteAllByUserId(teacherId);

            // 질문 삭제 — cascade ALL로 answer → likes 까지 모두 삭제
            questionIds.forEach(id -> questionRepository.findById(id).ifPresent(questionRepository::delete));

            // 유저/프로필 정리
            teacherProfileRepository.findById(teacherProfileId).ifPresent(teacherProfileRepository::delete);
            userRepository.findById(teacherId).ifPresent(userRepository::delete);
            userRepository.findById(studentId).ifPresent(userRepository::delete);
        });
    }

    @Test
    @DisplayName("선생님 답변 10개 동시 채택 — naegongScore가 정확히 100이어야 함")
    void concurrentAcceptAnswers_naegongScoreAccumulatesCorrectly() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(ANSWER_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(ANSWER_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);

        for (Long answerId : answerIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    qnaService.acceptAnswer(studentId, answerId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    // 채택 실패 시 successCount 부족으로 감지됨
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 10개 스레드 동시 출발
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        executor.awaitTermination(5, TimeUnit.SECONDS);

        assertThat(finished)
                .as("10개 스레드 모두 제한 시간(30초) 내에 완료되어야 합니다")
                .isTrue();

        assertThat(successCount.get())
                .as("10개 채택 모두 성공해야 합니다 (각각 다른 질문-답변 쌍이므로 충돌 없음)")
                .isEqualTo(ANSWER_COUNT);

        // 핵심 검증: naegongScore = 10 * 10 = 100
        TeacherProfile finalProfile = transactionTemplate.execute(
                status -> teacherProfileRepository.findById(teacherProfileId).orElseThrow());
        assertThat(finalProfile.getNaegongScore())
                .as("10번의 채택으로 naegongScore는 정확히 %d이어야 합니다 "
                        + "(비관적 락 없이는 lost update로 실제보다 작게 나옵니다)",
                        ANSWER_COUNT * SCORE_PER_ACCEPT)
                .isEqualTo(ANSWER_COUNT * SCORE_PER_ACCEPT);
    }
}
