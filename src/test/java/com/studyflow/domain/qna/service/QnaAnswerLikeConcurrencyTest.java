package com.studyflow.domain.qna.service;

import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaAnswerLike;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.repository.QnaAnswerLikeRepository;
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

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 답변에 20명이 동시에 좋아요를 눌렀을 때 likeCount가 정확히 20인지 검증합니다.
 *
 * likeCount는 QnaAnswer의 비정규화 카운터입니다.
 * 락 없이 read-modify-write를 수행하면 동시 요청이 모두 같은 초기값(0)을 읽고
 * 각자 1로 올려 저장하므로 last-write-wins로 인해 최종값이 1이 됩니다.
 *
 * findByIdWithLock(@Lock PESSIMISTIC_WRITE)을 통해 answer 행에 배타 락을 걸면
 * 20개 트랜잭션이 직렬화되어 0→1→2→...→20 순서로 카운터가 정확히 갱신됩니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class QnaAnswerLikeConcurrencyTest {

    private static final int LIKE_COUNT = 20;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired private QnaService qnaService;
    @Autowired private QnaAnswerRepository answerRepository;
    @Autowired private QnaAnswerLikeRepository likeRepository;
    @Autowired private QnaQuestionRepository questionRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TransactionTemplate transactionTemplate;

    private Long questionAuthorId;
    private Long answerAuthorId;
    private List<Long> likerIds;
    private Long questionId;
    private Long answerId;
    private String testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        // 질문 작성자 (학생)
        User questionAuthor = userRepository.save(
                User.createForTest("like-qauthor-" + testId + "@test.studyflow.com", "질문작성자", UserRole.STUDENT));
        questionAuthorId = questionAuthor.getId();

        // 답변 작성자 (선생님)
        User answerAuthor = userRepository.save(
                User.createForTest("like-aauthor-" + testId + "@test.studyflow.com", "답변작성자", UserRole.TEACHER));
        answerAuthorId = answerAuthor.getId();

        // 좋아요 누를 사용자 20명 (각자 다른 userId → unique constraint 충돌 없음)
        likerIds = new ArrayList<>();
        for (int i = 1; i <= LIKE_COUNT; i++) {
            User liker = userRepository.save(
                    User.createForTest("like-user" + i + "-" + testId + "@test.studyflow.com",
                            "좋아요유저" + i, UserRole.STUDENT));
            likerIds.add(liker.getId());
        }

        // 질문·답변 생성
        Subject subject = subjectRepository.findByParentSubjectIsNullOrderByIdAsc().get(0);
        transactionTemplate.executeWithoutResult(status -> {
            User qa = userRepository.findById(questionAuthorId).orElseThrow();
            User aa = userRepository.findById(answerAuthorId).orElseThrow();

            QnaQuestion question = questionRepository.save(
                    QnaQuestion.create(qa, subject, "좋아요 동시성 테스트 질문", "테스트 내용"));
            questionId = question.getId();

            QnaAnswer answer = answerRepository.save(
                    QnaAnswer.create(question, aa, "테스트 답변입니다."));
            answerId = answer.getId();
        });
    }

    @AfterEach
    void tearDown() {
        transactionTemplate.executeWithoutResult(status -> {
            // 알림 정리 (질문 작성자에게 전송된 답변/좋아요 알림)
            notificationRepository.deleteAllByRecipientId(questionAuthorId);

            // 질문 삭제 — cascade ALL로 answer → likes 까지 모두 삭제
            questionRepository.findById(questionId).ifPresent(questionRepository::delete);

            // 유저 정리
            likerIds.forEach(id -> userRepository.findById(id).ifPresent(userRepository::delete));
            userRepository.findById(answerAuthorId).ifPresent(userRepository::delete);
            userRepository.findById(questionAuthorId).ifPresent(userRepository::delete);
        });
    }

    @Test
    @DisplayName("동일 답변에 20명 동시 좋아요 — likeCount와 실제 좋아요 행 수가 모두 정확히 20이어야 함")
    void concurrentLikes_likeCountMatchesActualRows() throws InterruptedException {
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(LIKE_COUNT);
        ExecutorService executor = Executors.newFixedThreadPool(LIKE_COUNT);

        for (Long likerId : likerIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    qnaService.toggleAnswerLike(likerId, answerId);
                } catch (Exception e) {
                    // 좋아요 실패 시 테스트에서 카운트 불일치로 감지됨
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // 20개 스레드 동시 출발
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished)
                .as("20개 스레드 모두 제한 시간(30초) 내에 완료되어야 합니다")
                .isTrue();

        // 검증 1: 비정규화 카운터(likeCount)가 정확히 20인지
        QnaAnswer finalAnswer = transactionTemplate.execute(
                status -> answerRepository.findById(answerId).orElseThrow());
        assertThat(finalAnswer.getLikeCount())
                .as("likeCount 카운터가 정확히 %d이어야 합니다 (비관적 락 없이는 lost update로 실제보다 작게 나옵니다)", LIKE_COUNT)
                .isEqualTo(LIKE_COUNT);

        // 검증 2: 실제 QnaAnswerLike 행 수도 20인지 (카운터와 실제 데이터 일치 확인)
        long actualLikeRows = likeRepository.countByAnswerId(answerId);
        assertThat(actualLikeRows)
                .as("QnaAnswerLike 테이블의 실제 좋아요 행 수가 정확히 %d이어야 합니다", LIKE_COUNT)
                .isEqualTo(LIKE_COUNT);
    }

    @Test
    @DisplayName("좋아요 10명 + 미좋아요 10명 동시 토글 — likeCount는 10 유지, 좋아요 구성원은 교체")
    void concurrentMixedToggle_countUnchangedAndCompositionSwapped() throws InterruptedException {
        // likerIds 앞 10명: 이미 좋아요 상태로 사전 세팅
        // likerIds 뒤 10명: 좋아요 없는 상태 (BeforeEach 기본값 그대로)
        List<Long> alreadyLikedIds = likerIds.subList(0, 10);
        List<Long> notYetLikedIds  = likerIds.subList(10, 20);

        transactionTemplate.executeWithoutResult(status -> {
            QnaAnswer answer = answerRepository.findById(answerId).orElseThrow();
            for (Long userId : alreadyLikedIds) {
                User user = userRepository.findById(userId).orElseThrow();
                likeRepository.save(QnaAnswerLike.create(answer, user));
                answer.increaseLikeCount(); // dirty checking으로 커밋 시 자동 반영
            }
            // likeCount = 10 상태로 트랜잭션 커밋
        });

        // 20개 스레드 동시 출발
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch  = new CountDownLatch(LIKE_COUNT);
        ExecutorService executor  = Executors.newFixedThreadPool(LIKE_COUNT);

        for (Long likerId : likerIds) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    qnaService.toggleAnswerLike(likerId, answerId);
                } catch (Exception e) {
                    // 개별 실패 시 카운트 불일치로 감지됨
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        boolean finished = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        assertThat(finished)
                .as("20개 스레드 모두 제한 시간(30초) 내에 완료되어야 합니다")
                .isTrue();

        // 검증 1: likeCount = 10 유지 (-10 + 10 = 0 변화)
        QnaAnswer finalAnswer = transactionTemplate.execute(
                status -> answerRepository.findById(answerId).orElseThrow());
        assertThat(finalAnswer.getLikeCount())
                .as("취소 10 + 추가 10이 상쇄되어 likeCount는 10으로 유지되어야 합니다")
                .isEqualTo(10);

        // 검증 2: 실제 QnaAnswerLike 행 수도 10
        assertThat(likeRepository.countByAnswerId(answerId))
                .as("실제 좋아요 행 수도 10이어야 합니다")
                .isEqualTo(10);

        // 검증 3: 기존 좋아요 10명의 좋아요가 취소됨
        for (Long userId : alreadyLikedIds) {
            assertThat(likeRepository.findByAnswerIdAndUserId(answerId, userId))
                    .as("userId=%d — 기존 좋아요가 토글로 취소되어야 합니다", userId)
                    .isEmpty();
        }

        // 검증 4: 기존 미좋아요 10명에게 새 좋아요가 추가됨
        for (Long userId : notYetLikedIds) {
            assertThat(likeRepository.findByAnswerIdAndUserId(answerId, userId))
                    .as("userId=%d — 새 좋아요가 토글로 추가되어야 합니다", userId)
                    .isPresent();
        }
    }
}
