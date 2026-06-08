package com.studyflow.domain.qna.repository;

import com.studyflow.domain.qna.entity.QnaQuestion;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Constructor;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link QnaQuestionRepository}의 QueryDSL 조회(findFiltered/findDetailById) 통합 테스트. (H2)
 */
@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class QnaQuestionRepositoryTest {

    @Autowired QnaQuestionRepository questionRepository;
    @Autowired TestEntityManager em;

    // course 도메인의 JPA @Converter(NoticeAttachmentListConverter)가 ObjectMapper를 주입받으므로
    // @DataJpaTest 슬라이스에 ObjectMapper 빈을 보충해 컨텍스트 로딩을 가능하게 한다.
    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    private Long mathId;
    private Long koreanId;

    @BeforeEach
    void setUp() throws Exception {
        User student = persistUser("s@test.com", "학생", UserRole.STUDENT);
        Subject math = em.persist(Subject.ofCategory(SubjectCategory.MATH));
        Subject korean = em.persist(Subject.ofCategory(SubjectCategory.KOREAN));
        mathId = math.getId();
        koreanId = korean.getId();

        em.persist(QnaQuestion.create(student, math, "Derivative problem", "why derivative here"));
        QnaQuestion resolved = QnaQuestion.create(student, korean, "Poem analysis", "metaphor");
        resolved.markResolved();
        em.persist(resolved);
        em.persist(QnaQuestion.create(student, math, "Integral problem", "find the area"));

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("findFiltered: 필터 없으면 전체 반환")
    void findFiltered_noFilter_returnsAll() {
        assertThat(questionRepository.findFiltered(null, null, null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(3);
    }

    @Test
    @DisplayName("findFiltered: 과목으로 필터")
    void findFiltered_bySubject() {
        assertThat(questionRepository.findFiltered(mathId, null, null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("findFiltered: 해결여부로 필터")
    void findFiltered_byResolved() {
        assertThat(questionRepository.findFiltered(null, null, true, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(questionRepository.findFiltered(null, null, false, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(2);
    }

    @Test
    @DisplayName("findFiltered: 검색어는 제목 또는 내용에 매칭")
    void findFiltered_byKeyword_titleOrContent() {
        // 제목 매칭
        assertThat(questionRepository.findFiltered(null, "Derivative", null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        // 내용 매칭
        assertThat(questionRepository.findFiltered(null, "area", null, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        // 불일치
        assertThat(questionRepository.findFiltered(null, "nothing-xyz", null, PageRequest.of(0, 10)).getTotalElements())
                .isZero();
    }

    @Test
    @DisplayName("findFiltered: 과목+해결여부 복합 필터")
    void findFiltered_combined() {
        assertThat(questionRepository.findFiltered(koreanId, null, true, PageRequest.of(0, 10)).getTotalElements())
                .isEqualTo(1);
        assertThat(questionRepository.findFiltered(mathId, null, true, PageRequest.of(0, 10)).getTotalElements())
                .isZero();
    }

    @Test
    @DisplayName("findDetailById: 과목·작성자를 함께 로딩해 반환")
    void findDetailById_loadsSubjectAndAuthor() {
        Long id = questionRepository.findFiltered(mathId, "Derivative", null, PageRequest.of(0, 10))
                .getContent().get(0).getId();

        QnaQuestion found = questionRepository.findDetailById(id).orElseThrow();

        assertThat(found.getTitle()).isEqualTo("Derivative problem");
        assertThat(found.getSubject().getName()).isEqualTo("수학");
        assertThat(found.getAuthor().getName()).isEqualTo("학생");
    }

    @Test
    @DisplayName("findDetailById: 없으면 empty")
    void findDetailById_empty() {
        assertThat(questionRepository.findDetailById(99999L)).isEmpty();
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
