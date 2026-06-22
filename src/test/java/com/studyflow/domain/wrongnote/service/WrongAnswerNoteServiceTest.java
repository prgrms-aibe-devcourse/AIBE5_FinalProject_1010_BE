package com.studyflow.domain.wrongnote.service;

import com.studyflow.domain.ai.entity.AiQuestion;
import com.studyflow.domain.ai.repository.AiQuestionRepository;
import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.qna.repository.QnaQuestionRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteCreateRequest;
import com.studyflow.domain.wrongnote.dto.request.WrongAnswerNoteUpdateRequest;
import com.studyflow.domain.wrongnote.dto.response.WrongAnswerNoteResponse;
import com.studyflow.domain.wrongnote.entity.WrongAnswerNote;
import com.studyflow.domain.wrongnote.enums.WrongAnswerSourceType;
import com.studyflow.domain.wrongnote.repository.WrongAnswerNoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WrongAnswerNoteService 단위 테스트")
class WrongAnswerNoteServiceTest {

    @Mock WrongAnswerNoteRepository noteRepository;
    @Mock UserRepository userRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock QnaQuestionRepository qnaQuestionRepository;
    @Mock QnaAnswerRepository qnaAnswerRepository;
    @Mock AiQuestionRepository aiQuestionRepository;

    WrongAnswerNoteService service;

    static final Long USER_ID = 1L;
    static final Long OTHER_USER_ID = 99L;
    static final Long SUBJECT_ID = 3L;
    static final Long NOTE_ID = 10L;

    User owner;
    Subject subject;

    @BeforeEach
    void setUp() {
        service = new WrongAnswerNoteService(
                noteRepository,
                userRepository,
                subjectRepository,
                qnaQuestionRepository,
                qnaAnswerRepository,
                aiQuestionRepository
        );

        owner = user(USER_ID, "학생");
        subject = subject(SUBJECT_ID, "수학");

        lenient().when(userRepository.getReferenceById(USER_ID)).thenReturn(owner);
        lenient().when(subjectRepository.findById(SUBJECT_ID)).thenReturn(Optional.of(subject));
        lenient().when(noteRepository.save(any(WrongAnswerNote.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("직접 작성 오답노트를 저장하고 태그를 정규화한다")
    void createDirectNote() {
        WrongAnswerNoteCreateRequest request = new WrongAnswerNoteCreateRequest(
                SUBJECT_ID,
                "  이차방정식 오답  ",
                "x^2 = 4의 해를 구하시오.",
                "x = -2 또는 2",
                "양수 해만 적으면 안 된다.",
                "음수 해를 빼먹음",
                "다시 풀기",
                List.of("수학", "방정식", "수학", " "),
                WrongAnswerSourceType.DIRECT,
                null,
                null
        );

        WrongAnswerNoteResponse response = service.create(USER_ID, request);

        ArgumentCaptor<WrongAnswerNote> captor = ArgumentCaptor.forClass(WrongAnswerNote.class);
        verify(noteRepository).save(captor.capture());

        WrongAnswerNote saved = captor.getValue();
        assertThat(saved.isOwner(USER_ID)).isTrue();
        assertThat(saved.getTitle()).isEqualTo("이차방정식 오답");
        assertThat(saved.getQuestionContent()).isEqualTo("x^2 = 4의 해를 구하시오.");
        assertThat(saved.getSourceType()).isEqualTo(WrongAnswerSourceType.DIRECT);
        assertThat(saved.getTags()).containsExactly("수학", "방정식");

        assertThat(response.title()).isEqualTo("이차방정식 오답");
        assertThat(response.tags()).containsExactly("수학", "방정식");
    }

    @Test
    @DisplayName("QnA 질문에서 답변 ID를 생략하면 채택 답변을 복사해 오답노트를 만든다")
    void createFromQnaUsesAcceptedAnswer() {
        QnaQuestion question = qnaQuestion(100L, "미분 질문", "f'(x)를 어떻게 구하나요?");
        QnaAnswer accepted = qnaAnswer(200L, question, "도함수 정의를 사용하면 됩니다.");
        when(qnaQuestionRepository.findById(100L)).thenReturn(Optional.of(question));
        when(qnaAnswerRepository.findFirstByQuestionIdAndAcceptedTrue(100L)).thenReturn(Optional.of(accepted));

        WrongAnswerNoteCreateRequest request = new WrongAnswerNoteCreateRequest(
                null,
                null,
                null,
                null,
                "복습 필요",
                null,
                null,
                List.of("미분"),
                WrongAnswerSourceType.QNA,
                100L,
                null
        );

        WrongAnswerNoteResponse response = service.create(USER_ID, request);

        assertThat(response.sourceType()).isEqualTo(WrongAnswerSourceType.QNA);
        assertThat(response.sourceQuestionId()).isEqualTo(100L);
        assertThat(response.sourceAnswerId()).isEqualTo(200L);
        assertThat(response.subjectId()).isEqualTo(SUBJECT_ID);
        assertThat(response.title()).isEqualTo("미분 질문");
        assertThat(response.questionContent()).isEqualTo("f'(x)를 어떻게 구하나요?");
        assertThat(response.answerContent()).isEqualTo("도함수 정의를 사용하면 됩니다.");
        assertThat(response.explanation()).isEqualTo("복습 필요");
    }

    @Test
    @DisplayName("AI 질문은 본인 기록만 오답노트로 복사할 수 있다")
    void createFromAiRejectsOtherUsersRecord() {
        User other = user(OTHER_USER_ID, "다른 사용자");
        AiQuestion aiQuestion = AiQuestion.create(other, subject, null, "영어 지문 해석", "핵심 문장은 ...");
        setId(aiQuestion, 300L);
        when(aiQuestionRepository.findById(300L)).thenReturn(Optional.of(aiQuestion));

        WrongAnswerNoteCreateRequest request = new WrongAnswerNoteCreateRequest(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                WrongAnswerSourceType.AI,
                300L,
                null
        );

        assertThatThrownBy(() -> service.create(USER_ID, request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
    }

    @Test
    @DisplayName("AI 질문을 복사하면 질문과 답변 스냅샷을 저장한다")
    void createFromAiCopiesQuestionAndAnswer() {
        AiQuestion aiQuestion = AiQuestion.create(owner, subject, null, "영어 지문 해석", "핵심 문장은 주제문입니다.");
        setId(aiQuestion, 300L);
        when(aiQuestionRepository.findById(300L)).thenReturn(Optional.of(aiQuestion));

        WrongAnswerNoteResponse response = service.create(USER_ID, new WrongAnswerNoteCreateRequest(
                null,
                null,
                null,
                null,
                null,
                "주제문을 못 찾음",
                null,
                List.of("영어"),
                WrongAnswerSourceType.AI,
                300L,
                null
        ));

        assertThat(response.sourceType()).isEqualTo(WrongAnswerSourceType.AI);
        assertThat(response.sourceQuestionId()).isEqualTo(300L);
        assertThat(response.questionContent()).isEqualTo("영어 지문 해석");
        assertThat(response.answerContent()).isEqualTo("핵심 문장은 주제문입니다.");
        assertThat(response.wrongReason()).isEqualTo("주제문을 못 찾음");
    }

    @Test
    @DisplayName("수정 요청에서 tags를 생략하면 기존 태그를 유지한다")
    void updateKeepsTagsWhenTagsOmitted() {
        WrongAnswerNote note = noteWithId(NOTE_ID, List.of("수학", "오답"));
        when(noteRepository.findByIdAndOwnerIdAndDeletedAtIsNull(NOTE_ID, USER_ID)).thenReturn(Optional.of(note));

        WrongAnswerNoteUpdateRequest request = new WrongAnswerNoteUpdateRequest(
                null,
                "수정된 제목",
                "수정된 문제",
                null,
                null,
                null,
                null,
                null
        );

        WrongAnswerNoteResponse response = service.update(NOTE_ID, USER_ID, request);

        assertThat(response.title()).isEqualTo("수정된 제목");
        assertThat(response.questionContent()).isEqualTo("수정된 문제");
        assertThat(response.tags()).containsExactly("수학", "오답");
    }

    @Test
    @DisplayName("내 오답노트만 상세 조회할 수 있다")
    void getMyNote() {
        WrongAnswerNote note = noteWithId(NOTE_ID, List.of("태그"));
        when(noteRepository.findByIdAndOwnerIdAndDeletedAtIsNull(NOTE_ID, USER_ID)).thenReturn(Optional.of(note));

        WrongAnswerNoteResponse response = service.getMyNote(NOTE_ID, USER_ID);

        assertThat(response.id()).isEqualTo(NOTE_ID);
        assertThat(response.ownerId()).isEqualTo(USER_ID);
    }

    @Test
    @DisplayName("목록 조회는 ownerId, subjectId, keyword, pageable을 repository에 위임한다")
    void getMyNotesDelegatesToRepository() {
        PageRequest pageable = PageRequest.of(0, 20);
        WrongAnswerNote note = noteWithId(NOTE_ID, List.of());
        when(noteRepository.searchMine(USER_ID, SUBJECT_ID, "미분", pageable))
                .thenReturn(new PageImpl<>(List.of(note), pageable, 1));

        Page<WrongAnswerNoteResponse> page = service.getMyNotes(USER_ID, SUBJECT_ID, "  미분  ", pageable);

        assertThat(page.getContent()).hasSize(1);
        verify(noteRepository).searchMine(USER_ID, SUBJECT_ID, "미분", pageable);
    }

    @Test
    @DisplayName("삭제는 실제 삭제 대신 deletedAt을 채운다")
    void deleteSoftDeletes() {
        WrongAnswerNote note = noteWithId(NOTE_ID, List.of());
        when(noteRepository.findByIdAndOwnerIdAndDeletedAtIsNull(NOTE_ID, USER_ID)).thenReturn(Optional.of(note));

        service.delete(NOTE_ID, USER_ID);

        assertThat(note.getDeletedAt()).isNotNull();
    }

    private User user(Long id, String name) {
        User user = mock(User.class);
        lenient().when(user.getId()).thenReturn(id);
        lenient().when(user.getName()).thenReturn(name);
        return user;
    }

    private Subject subject(Long id, String name) {
        Subject subject = mock(Subject.class);
        lenient().when(subject.getId()).thenReturn(id);
        lenient().when(subject.getName()).thenReturn(name);
        return subject;
    }

    private QnaQuestion qnaQuestion(Long id, String title, String content) {
        QnaQuestion question = QnaQuestion.create(owner, subject, title, content);
        setId(question, id);
        return question;
    }

    private QnaAnswer qnaAnswer(Long id, QnaQuestion question, String content) {
        QnaAnswer answer = QnaAnswer.create(question, user(2L, "선생님"), content);
        setId(answer, id);
        answer.accept();
        return answer;
    }

    private WrongAnswerNote noteWithId(Long id, List<String> tags) {
        WrongAnswerNote note = WrongAnswerNote.create(
                owner,
                subject,
                "원래 제목",
                "원래 문제",
                "원래 답",
                null,
                null,
                null,
                WrongAnswerSourceType.DIRECT,
                null,
                null,
                null,
                tags
        );
        setId(note, id);
        return note;
    }

    private void setId(Object target, Long id) {
        ReflectionTestUtils.setField(target, "id", id);
    }
}
