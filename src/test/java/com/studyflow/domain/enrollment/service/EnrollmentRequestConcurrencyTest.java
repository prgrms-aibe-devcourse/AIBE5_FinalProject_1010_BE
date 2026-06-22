package com.studyflow.domain.enrollment.service;

import com.studyflow.domain.course.entity.Course;
import com.studyflow.domain.course.enums.CurriculumType;
import com.studyflow.domain.course.enums.TargetGrade;
import com.studyflow.domain.course.enums.TeachingMode;
import com.studyflow.domain.course.repository.CourseRepository;
import com.studyflow.domain.enrollment.entity.EnrollmentRequest;
import com.studyflow.domain.enrollment.enums.EnrollmentRequestStatus;
import com.studyflow.domain.enrollment.enums.EnrollmentStatus;
import com.studyflow.domain.enrollment.repository.EnrollmentRepository;
import com.studyflow.domain.enrollment.repository.EnrollmentRequestRepository;
import com.studyflow.domain.notification.repository.NotificationRepository;
import com.studyflow.domain.student.entity.StudentProfile;
import com.studyflow.domain.student.repository.StudentProfileRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.teacher.entity.TeacherProfile;
import com.studyflow.domain.teacher.repository.TeacherProfileRepository;
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

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동일 수강신청에 대해 학생의 취소와 선생님의 수락이 동시에 발생할 때
 * 정확히 하나만 성공하고 하나는 실패하는지 검증합니다.
 *
 * 비관적 락(@Lock PESSIMISTIC_WRITE)이 적용된 findByIdWithCourse /
 * findByIdWithUserAndCourse 덕분에, 먼저 락을 획득한 트랜잭션만 상태를 변경하고
 * 나머지는 PENDING이 아닌 상태를 보고 예외를 던집니다.
 *
 * 10라운드 반복 → 모든 라운드에서 성공=1 / 실패=1 이어야만 테스트가 통과합니다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class EnrollmentRequestConcurrencyTest {

    // 외부 의존성 — 컨텍스트 로딩 실패 방지
    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean
    private ClientRegistrationRepository clientRegistrationRepository;

    @MockitoBean
    private SimpMessagingTemplate messagingTemplate;

    @Autowired private EnrollmentRequestService enrollmentRequestService;
    @Autowired private TransactionTemplate transactionTemplate;

    @Autowired private UserRepository userRepository;
    @Autowired private StudentProfileRepository studentProfileRepository;
    @Autowired private TeacherProfileRepository teacherProfileRepository;
    @Autowired private SubjectRepository subjectRepository;
    @Autowired private CourseRepository courseRepository;
    @Autowired private EnrollmentRequestRepository enrollmentRequestRepository;
    @Autowired private EnrollmentRepository enrollmentRepository;
    @Autowired private NotificationRepository notificationRepository;

    private Long studentId;
    private Long teacherId;
    private Long courseId;
    // 테스트 메서드 간 이메일 충돌 방지 — AfterEach 실패 시에도 독립성 보장
    private String testId;

    @BeforeEach
    void setUp() {
        testId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);

        User student = userRepository.save(
                User.createForTest("c-student-" + testId + "@test.studyflow.com", "동시성테스트학생", UserRole.STUDENT));
        studentId = student.getId();
        studentProfileRepository.save(StudentProfile.createForUser(student));

        User teacher = userRepository.save(
                User.createForTest("c-teacher-" + testId + "@test.studyflow.com", "동시성테스트선생님", UserRole.TEACHER));
        teacherId = teacher.getId();
        TeacherProfile tp = teacherProfileRepository.save(TeacherProfile.createForUser(teacher));

        // SubjectDataInitializer가 시딩한 과목 중 첫 번째 사용
        Subject subject = subjectRepository.findByParentSubjectIsNullOrderByIdAsc().get(0);

        Course course = Course.create(
                tp, subject,
                "동시성 테스트 수업", null,
                TargetGrade.HIGH_1, 5, 60, 50000,
                null, CurriculumType.CUSTOM, null,
                null, null, null,
                null, null, null,
                TeachingMode.ONLINE, null, null, null
        );
        courseId = courseRepository.save(course).getId();
    }

    @AfterEach
    void tearDown() {
        // TransactionTemplate을 사용해 명시적으로 트랜잭션 내에서 실행합니다.
        // 클래스 레벨 @Transactional이 없는 동시성 테스트에서는
        // @Transactional on @AfterEach가 동작하지 않으므로 직접 트랜잭션을 관리합니다.
        transactionTemplate.executeWithoutResult(status -> {
            // 알림 — 학생·선생님 모두 수신자가 될 수 있음
            notificationRepository.deleteAllByRecipientId(studentId);
            notificationRepository.deleteAllByRecipientId(teacherId);

            // 수강 이력
            enrollmentRepository.findWithUserByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)
                    .forEach(enrollmentRepository::delete);

            // 수강 신청 이력 — 트랜잭션 내에서 getCourse() lazy 로딩 가능
            enrollmentRequestRepository.findAll().stream()
                    .filter(r -> r.getCourse().getId().equals(courseId))
                    .forEach(enrollmentRequestRepository::delete);

            // 수업 → 프로필 → 유저 순서로 삭제
            courseRepository.deleteById(courseId);
            teacherProfileRepository.findByUserId(teacherId).ifPresent(teacherProfileRepository::delete);
            studentProfileRepository.findByUserId(studentId).ifPresent(studentProfileRepository::delete);
            userRepository.findById(teacherId).ifPresent(userRepository::delete);
            userRepository.findById(studentId).ifPresent(userRepository::delete);
        });
    }

    @Test
    @DisplayName("동시 수강신청 취소(학생) + 수락(선생님): 10회 반복 — 매 라운드 정확히 하나만 성공")
    void concurrentCancelAndAccept_exactlyOneSucceedsPerRound() throws InterruptedException {
        for (int round = 1; round <= 10; round++) {
            Long requestId = createPendingRequest();

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            // 학생 취소 스레드
            executor.submit(() -> {
                try {
                    startLatch.await();
                    enrollmentRequestService.cancelEnrollmentRequest(requestId, studentId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            // 선생님 수락 스레드
            executor.submit(() -> {
                try {
                    startLatch.await();
                    enrollmentRequestService.acceptEnrollmentRequest(requestId, teacherId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown(); // 두 스레드 동시 출발
            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(finished)
                    .as("Round %d — 두 스레드가 제한 시간(10초) 내에 완료되어야 합니다", round)
                    .isTrue();
            assertThat(successCount.get())
                    .as("Round %d — 정확히 하나의 작업만 성공해야 합니다", round)
                    .isEqualTo(1);
            assertThat(failCount.get())
                    .as("Round %d — 정확히 하나의 작업은 실패해야 합니다", round)
                    .isEqualTo(1);

            // DB 상태: PENDING이 남아서는 안 됨 (둘 중 하나가 반드시 상태를 변경했어야 함)
            EnrollmentRequest finalRequest = enrollmentRequestRepository.findById(requestId).orElseThrow();
            assertThat(finalRequest.getStatus())
                    .as("Round %d — EnrollmentRequest 상태가 PENDING 그대로여서는 안 됩니다", round)
                    .isNotEqualTo(EnrollmentRequestStatus.PENDING);

            // 라운드 간 고유 제약 충돌 방지를 위한 데이터 정리
            cleanupRound(requestId);
        }
    }

    @Test
    @DisplayName("동시 수강신청 취소(학생) + 거절(선생님): 10회 반복 — 매 라운드 정확히 하나만 성공")
    void concurrentCancelAndReject_exactlyOneSucceedsPerRound() throws InterruptedException {
        for (int round = 1; round <= 10; round++) {
            Long requestId = createPendingRequest();

            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(2);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger failCount = new AtomicInteger(0);

            ExecutorService executor = Executors.newFixedThreadPool(2);

            executor.submit(() -> {
                try {
                    startLatch.await();
                    enrollmentRequestService.cancelEnrollmentRequest(requestId, studentId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            executor.submit(() -> {
                try {
                    startLatch.await();
                    enrollmentRequestService.rejectEnrollmentRequest(requestId, teacherId);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    doneLatch.countDown();
                }
            });

            startLatch.countDown();
            boolean finished = doneLatch.await(10, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(5, TimeUnit.SECONDS);

            assertThat(finished)
                    .as("Round %d — 두 스레드가 제한 시간(10초) 내에 완료되어야 합니다", round)
                    .isTrue();
            assertThat(successCount.get())
                    .as("Round %d — 정확히 하나의 작업만 성공해야 합니다", round)
                    .isEqualTo(1);
            assertThat(failCount.get())
                    .as("Round %d — 정확히 하나의 작업은 실패해야 합니다", round)
                    .isEqualTo(1);

            EnrollmentRequest finalRequest = enrollmentRequestRepository.findById(requestId).orElseThrow();
            assertThat(finalRequest.getStatus())
                    .as("Round %d — EnrollmentRequest 상태가 PENDING 그대로여서는 안 됩니다", round)
                    .isNotEqualTo(EnrollmentRequestStatus.PENDING);

            cleanupRound(requestId);
        }
    }

    // ─── 헬퍼 ────────────────────────────────────────────────────────────────

    /** 매 라운드마다 새 PENDING 수강신청을 직접 생성합니다 (서비스 검증 우회). */
    private Long createPendingRequest() {
        User student = userRepository.findById(studentId).orElseThrow();
        Course course = courseRepository.findById(courseId).orElseThrow();
        EnrollmentRequest req = EnrollmentRequest.create(course, student, null, null, null, null, null);
        return enrollmentRequestRepository.save(req).getId();
    }

    /**
     * 라운드 간 고유 제약 충돌 방지 정리.
     * (user_id, course_id, status)의 유니크 제약 때문에 같은 조합의 상태가
     * 두 번 이상 생기지 않도록 라운드 종료 시 수강신청(과 생성된 수강 이력)을 삭제합니다.
     */
    private void cleanupRound(Long requestId) {
        transactionTemplate.executeWithoutResult(status -> {
            // 수락이 성공했다면 Enrollment도 생성되어 있음
            enrollmentRepository.findWithUserByCourseIdAndStatus(courseId, EnrollmentStatus.ACTIVE)
                    .forEach(enrollmentRepository::delete);
            enrollmentRequestRepository.deleteById(requestId);
        });
    }
}
