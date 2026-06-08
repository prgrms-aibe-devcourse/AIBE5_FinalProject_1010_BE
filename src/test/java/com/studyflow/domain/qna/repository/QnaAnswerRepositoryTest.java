package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaAnswerLike;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.repository.QnaAnswerRepositoryCustom.QuestionAnswerCount;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.Gender;
import com.studyflow.domain.user.enums.SocialProvider;
import com.studyflow.domain.user.enums.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.global.config.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link QnaAnswerRepository}·{@link QnaAnswerLikeRepository}의 QueryDSL 조회 통합 테스트. (H2)
 */
@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class QnaAnswerRepositoryTest {

    @Autowired QnaAnswerRepository answerRepository;
    @Autowired QnaAnswerLikeRepository likeRepository;
    @Autowired TestEntityManager em;

    // course 도메인의 JPA @Converter가 ObjectMapper를 주입받으므로 슬라이스 컨텍스트에 보충한다.
    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private Long questionId;
    private Long studentId;
    private Long a1Id; // teacher1, like 0
    private Long a2Id; // teacher2, accepted
    private Long a3Id; // teacher1, like 2

    @BeforeEach
    void setUp() throws Exception {
        User student = persistUser("s@test.com", "학생", UserRole.STUDENT);
        User teacher1 = persistUser("t1@test.com", "선생1", UserRole.TEACHER);
        User teacher2 = persistUser("t2@test.com", "선생2", UserRole.TEACHER);
        Subject math = em.persist(Subject.ofCategory(SubjectCategory.MATH));
        studentId = student.getId();

        QnaQuestion q = em.persist(QnaQuestion.create(student, math, "title", "content"));
        questionId = q.getId();

        QnaAnswer a1 = QnaAnswer.create(q, teacher1, "answer 1"); // like 0
        QnaAnswer a2 = QnaAnswer.create(q, teacher2, "answer 2"); // accepted
        a2.accept();
        QnaAnswer a3 = QnaAnswer.create(q, teacher1, "answer 3"); // like 2
        a3.increaseLikeCount();
        a3.increaseLikeCount();
        em.persist(a1);
        em.persist(a2);
        em.persist(a3);
        a1Id = a1.getId();
        a2Id = a2.getId();
        a3Id = a3.getId();

        // 학생이 a3에만 좋아요
        em.persist(QnaAnswerLike.create(a3, student));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findByQuestionIdWithAuthor: 채택 → 좋아요 많은 순 → 오래된 순으로 정렬하고 작성자를 로딩")
    void findByQuestionIdWithAuthor_orderedAndAuthorLoaded() {
        List<QnaAnswer> answers = answerRepository.findByQuestionIdWithAuthor(questionId);

        assertThat(answers).extracting(QnaAnswer::getId)
                .containsExactly(a2Id, a3Id, a1Id); // 채택(a2) → like2(a3) → like0(a1)
        // 작성자 fetch join 확인 (LazyInitException 없이 접근)
        assertThat(answers.get(0).getAuthor().getName()).isEqualTo("선생2");
    }

    @Test
    @DisplayName("countByQuestionIds: 질문별 답변 개수를 집계")
    void countByQuestionIds_aggregates() {
        List<QuestionAnswerCount> counts = answerRepository.countByQuestionIds(List.of(questionId));

        assertThat(counts).hasSize(1);
        assertThat(counts.get(0).questionId()).isEqualTo(questionId);
        assertThat(counts.get(0).cnt()).isEqualTo(3L);
    }

    @Test
    @DisplayName("findDetailById: 질문·질문작성자·답변작성자를 함께 로딩")
    void findDetailById_loadsRelations() {
        QnaAnswer found = answerRepository.findDetailById(a1Id).orElseThrow();

        assertThat(found.getContent()).isEqualTo("answer 1");
        assertThat(found.getAuthor().getName()).isEqualTo("선생1");
        assertThat(found.getQuestion().getAuthor().getName()).isEqualTo("학생");
    }

    @Test
    @DisplayName("findLikedAnswerIds: 사용자가 좋아요한 답변 id만 반환")
    void findLikedAnswerIds_returnsOnlyLiked() {
        List<Long> liked = likeRepository.findLikedAnswerIds(studentId, List.of(a1Id, a2Id, a3Id));

        assertThat(liked).containsExactly(a3Id);
    }

    private User persistUser(String email, String name, UserRole role) throws Exception {
        Constructor<User> ctor = User.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        User user = ctor.newInstance();
        ReflectionTestUtils.setField(user, "email", email);
        ReflectionTestUtils.setField(user, "name", name);
        ReflectionTestUtils.setField(user, "role", role);
        ReflectionTestUtils.setField(user, "socialProvider", SocialProvider.LOCAL);
        ReflectionTestUtils.setField(user, "gender", Gender.MALE);
        ReflectionTestUtils.setField(user, "birthDate", LocalDate.of(2000, 1, 1));
        ReflectionTestUtils.setField(user, "isActive", true);
        ReflectionTestUtils.setField(user, "isDeleted", 0L);
        return em.persist(user);
    }
}
