package com.studyflow.domain.wrongnote.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.enums.UserRole;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNoteReview;
import com.studyflow.domain.wrongnote.enums.WrongAnswerReviewResult;
import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;
import com.studyflow.global.config.QuerydslConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(QuerydslConfig.class)
@TestPropertySource(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
class WrongAnswerNoteRepositoryTest {

    @Autowired WrongAnswerNoteRepository noteRepository;
    @Autowired WrongAnswerNoteReviewRepository reviewRepository;
    @Autowired TestEntityManager em;

    @TestConfiguration
    static class TestBeans {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
    }

    Long ownerId;
    Long subjectId;
    Long newNoteId;
    Long dueNoteId;
    Long futureNoteId;

    User owner;
    Subject subject;

    @BeforeEach
    void setUp() {
        owner = em.persist(User.createForTest("student@test.com", "학생", UserRole.STUDENT));
        subject = em.persist(Subject.ofCategory(SubjectCategory.MATH));
        ownerId = owner.getId();
        subjectId = subject.getId();

        WrongAnswerNote newNote = em.persist(note("새 문제", "아직 복습하지 않은 문제"));

        WrongAnswerNote dueNote = note("오답 문제", "다시 볼 시점이 지난 문제");
        dueNote.recordReview(WrongAnswerReviewResult.INCORRECT, LocalDateTime.now().minusDays(2));
        dueNote = em.persist(dueNote);

        WrongAnswerNote futureNote = note("정답 문제", "아직 다음 복습 전인 문제");
        futureNote.recordReview(WrongAnswerReviewResult.CORRECT, LocalDateTime.now());
        futureNote = em.persist(futureNote);

        newNoteId = newNote.getId();
        dueNoteId = dueNote.getId();
        futureNoteId = futureNote.getId();

        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("문제풀이 추천은 미복습, 복습 예정 초과, 향후 복습 순으로 조회한다")
    void findPracticeRecommendations() {
        List<WrongAnswerNote> result = noteRepository.findPracticeRecommendations(
                ownerId,
                subjectId,
                LocalDateTime.now(),
                PageRequest.of(0, 10)
        );

        assertThat(result).extracting(WrongAnswerNote::getId)
                .containsExactly(newNoteId, dueNoteId, futureNoteId);
    }

    @Test
    @DisplayName("삭제된 오답노트는 문제풀이 추천에서 제외한다")
    void findPracticeRecommendationsExcludesDeletedNotes() {
        WrongAnswerNote deleted = note("삭제된 문제", "추천되면 안 되는 문제");
        deleted.delete(LocalDateTime.now());
        em.persist(deleted);
        em.flush();
        em.clear();

        List<WrongAnswerNote> result = noteRepository.findPracticeRecommendations(
                ownerId,
                subjectId,
                LocalDateTime.now(),
                PageRequest.of(0, 10)
        );

        assertThat(result).extracting(WrongAnswerNote::getTitle)
                .doesNotContain("삭제된 문제");
    }

    @Test
    @DisplayName("복습 기록은 본인 오답노트 기준으로 최신순 조회한다")
    void findReviewLogs() {
        WrongAnswerNote note = em.find(WrongAnswerNote.class, dueNoteId);
        LocalDateTime older = LocalDateTime.now().minusDays(1);
        LocalDateTime newer = LocalDateTime.now();
        em.persist(WrongAnswerNoteReview.create(note, owner, WrongAnswerReviewResult.UNSURE, "헷갈림", older));
        em.persist(WrongAnswerNoteReview.create(note, owner, WrongAnswerReviewResult.CORRECT, "이제 맞음", newer));
        em.flush();
        em.clear();

        Page<WrongAnswerNoteReview> result = reviewRepository.findByNoteIdAndNoteOwnerIdAndResultNotOrderByReviewedAtDesc(
                dueNoteId,
                ownerId,
                WrongAnswerReviewResult.INCORRECT,
                PageRequest.of(0, 10)
        );

        assertThat(result.getContent()).extracting(WrongAnswerNoteReview::getResult)
                .containsExactly(WrongAnswerReviewResult.CORRECT, WrongAnswerReviewResult.UNSURE);
    }

    private WrongAnswerNote note(String title, String question) {
        return WrongAnswerNote.create(
                owner,
                subject,
                title,
                question,
                "정답",
                "풀이",
                "틀린 이유",
                null,
                WrongAnswerSourceType.DIRECT,
                null,
                null,
                null,
                List.of("수학")
        );
    }
}
