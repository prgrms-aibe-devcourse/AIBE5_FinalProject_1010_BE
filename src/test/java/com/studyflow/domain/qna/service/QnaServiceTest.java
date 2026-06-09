package com.studyflow.domain.qna.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.naegong.enums.NaegongReason;
import com.studyflow.domain.naegong.service.NaegongService;
import com.studyflow.domain.qna.dto.request.QnaAnswerRequest;
import com.studyflow.domain.qna.dto.request.QnaQuestionCreateRequest;
import com.studyflow.domain.qna.dto.request.QnaQuestionUpdateRequest;
import com.studyflow.domain.qna.dto.response.QnaAnswerAcceptResponse;
import com.studyflow.domain.qna.dto.response.QnaLikeResponse;
import com.studyflow.domain.qna.dto.response.QnaQuestionCreateResponse;
import com.studyflow.domain.qna.dto.response.QnaQuestionDetailResponse;
import com.studyflow.domain.qna.dto.response.QnaQuestionSummaryResponse;
import com.studyflow.domain.qna.entity.QnaAnswer;
import com.studyflow.domain.qna.entity.QnaAnswerLike;
import com.studyflow.domain.qna.entity.QnaQuestion;
import com.studyflow.domain.qna.exception.QnaForbiddenException;
import com.studyflow.domain.qna.exception.QnaInvalidStateException;
import com.studyflow.domain.qna.exception.QnaQuestionNotFoundException;
import com.studyflow.domain.qna.repository.QnaAnswerAttachmentRepository;
import com.studyflow.domain.qna.repository.QnaAnswerLikeRepository;
import com.studyflow.domain.qna.repository.QnaAnswerRepository;
import com.studyflow.domain.qna.repository.QnaAnswerRepositoryCustom.QuestionAnswerCount;
import com.studyflow.domain.qna.repository.QnaQuestionAttachmentRepository;
import com.studyflow.domain.qna.repository.QnaQuestionRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.exception.SubjectNotFoundException;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link QnaService} 단위 테스트.
 *
 * <p>리포지토리·내공 서비스를 목으로 대체해 CRUD/권한/채택(내공 적립)/좋아요/첨부 검증 흐름을
 * 외부 의존 없이 확인한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class QnaServiceTest {

    @Mock QnaQuestionRepository questionRepository;
    @Mock QnaAnswerRepository answerRepository;
    @Mock QnaQuestionAttachmentRepository questionAttachmentRepository;
    @Mock QnaAnswerAttachmentRepository answerAttachmentRepository;
    @Mock QnaAnswerLikeRepository likeRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock UserRepository userRepository;
    @Mock FileAssetRepository fileAssetRepository;
    @Mock NaegongService naegongService;
    @Mock ObjectMapper objectMapper;

    @InjectMocks QnaService service;

    // ── 헬퍼 ─────────────────────────────────────────────────

    private User mockUser(Long id) {
        User user = mock(User.class);
        // lenient: 일부 테스트에선 이 user의 getId가 쓰이지 않을 수 있다(예: 권한 체크 전 예외).
        lenient().when(user.getId()).thenReturn(id);
        return user;
    }

    private QnaQuestion question(Long id, User author, Subject subject) {
        QnaQuestion q = QnaQuestion.create(author, subject, "title", "content");
        ReflectionTestUtils.setField(q, "id", id);
        return q;
    }

    private QnaAnswer answer(Long id, QnaQuestion question, User author) {
        QnaAnswer a = QnaAnswer.create(question, author, "answer content");
        ReflectionTestUtils.setField(a, "id", id);
        return a;
    }

    private QnaQuestionCreateRequest createReq(Long subjectId, String title, String content, List<Long> fileIds) {
        QnaQuestionCreateRequest r = new QnaQuestionCreateRequest();
        ReflectionTestUtils.setField(r, "subjectId", subjectId);
        ReflectionTestUtils.setField(r, "title", title);
        ReflectionTestUtils.setField(r, "content", content);
        ReflectionTestUtils.setField(r, "imageFileIds", fileIds);
        return r;
    }

    private QnaAnswerRequest answerReq(String content) {
        QnaAnswerRequest r = new QnaAnswerRequest();
        ReflectionTestUtils.setField(r, "content", content);
        return r;
    }

    // ── 질문 작성 ─────────────────────────────────────────────

    @Test
    @DisplayName("createQuestion: 과목·작성자를 채워 질문을 저장한다")
    void createQuestion_savesWithAuthorAndSubject() {
        Subject subject = mock(Subject.class);
        User author = mockUser(1L);
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(subject));
        when(userRepository.getReferenceById(1L)).thenReturn(author);
        when(questionRepository.save(any(QnaQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        QnaQuestionCreateResponse res = service.createQuestion(1L, createReq(3L, "미분", "도함수?", null));

        ArgumentCaptor<QnaQuestion> captor = ArgumentCaptor.forClass(QnaQuestion.class);
        verify(questionRepository).save(captor.capture());
        assertThat(captor.getValue().getTitle()).isEqualTo("미분");
        assertThat(captor.getValue().getContent()).isEqualTo("도함수?");
        assertThat(captor.getValue().isAuthor(1L)).isTrue();
        assertThat(res.isResolved()).isFalse();
    }

    @Test
    @DisplayName("createQuestion: 존재하지 않는 과목이면 SubjectNotFoundException, 저장하지 않는다")
    void createQuestion_subjectNotFound() {
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createQuestion(1L, createReq(99L, "t", "c", null)))
                .isInstanceOf(SubjectNotFoundException.class);

        verify(questionRepository, never()).save(any());
    }

    @Test
    @DisplayName("createQuestion: 첨부 이미지가 본인 소유가 아니면 거부하고 저장하지 않는다")
    void createQuestion_rejectsForeignImage() {
        User author = mockUser(1L);
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(mock(Subject.class)));
        when(userRepository.getReferenceById(1L)).thenReturn(author);

        FileAsset foreign = mock(FileAsset.class);
        when(foreign.getId()).thenReturn(12L);
        User otherOwner = mock(User.class);
        when(otherOwner.getId()).thenReturn(999L);
        when(foreign.getUploader()).thenReturn(otherOwner);
        when(fileAssetRepository.findByIdInWithUploader(List.of(12L))).thenReturn(List.of(foreign));

        assertThatThrownBy(() -> service.createQuestion(1L, createReq(3L, "t", "c", List.of(12L))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(questionRepository, never()).save(any());
    }

    // ── 질문 수정/삭제 권한 ────────────────────────────────────

    @Test
    @DisplayName("updateQuestion: 작성자가 아니면 QnaForbiddenException")
    void updateQuestion_notAuthor_forbidden() {
        QnaQuestion q = question(5L, mockUser(2L), mock(Subject.class)); // 작성자=2
        when(questionRepository.findById(5L)).thenReturn(Optional.of(q));

        QnaQuestionUpdateRequest req = new QnaQuestionUpdateRequest();
        ReflectionTestUtils.setField(req, "subjectId", 3L);
        ReflectionTestUtils.setField(req, "title", "t");
        ReflectionTestUtils.setField(req, "content", "c");

        assertThatThrownBy(() -> service.updateQuestion(1L, 5L, req)) // 호출자=1
                .isInstanceOf(QnaForbiddenException.class);
    }

    @Test
    @DisplayName("deleteQuestion: 관리자는 타인 질문도 삭제할 수 있다")
    void deleteQuestion_adminCanDeleteOthers() {
        QnaQuestion q = question(5L, mockUser(2L), mock(Subject.class));
        when(questionRepository.findById(5L)).thenReturn(Optional.of(q));

        service.deleteQuestion(1L, 5L, true); // isAdmin=true

        verify(questionRepository).delete(q);
    }

    @Test
    @DisplayName("deleteQuestion: 작성자도 관리자도 아니면 삭제 불가")
    void deleteQuestion_notAuthorNotAdmin_forbidden() {
        QnaQuestion q = question(5L, mockUser(2L), mock(Subject.class));
        when(questionRepository.findById(5L)).thenReturn(Optional.of(q));

        assertThatThrownBy(() -> service.deleteQuestion(1L, 5L, false))
                .isInstanceOf(QnaForbiddenException.class);
        verify(questionRepository, never()).delete(any());
    }

    // ── 답변 작성 ─────────────────────────────────────────────

    @Test
    @DisplayName("createAnswer: 질문에 답변을 저장한다")
    void createAnswer_saves() {
        QnaQuestion q = question(5L, mockUser(1L), mock(Subject.class));
        User teacher = mockUser(2L);
        when(questionRepository.findById(5L)).thenReturn(Optional.of(q));
        when(userRepository.getReferenceById(2L)).thenReturn(teacher);
        when(answerRepository.save(any(QnaAnswer.class))).thenAnswer(inv -> inv.getArgument(0));

        service.createAnswer(2L, 5L, answerReq("이렇게 풉니다"));

        ArgumentCaptor<QnaAnswer> captor = ArgumentCaptor.forClass(QnaAnswer.class);
        verify(answerRepository).save(captor.capture());
        assertThat(captor.getValue().getContent()).isEqualTo("이렇게 풉니다");
        assertThat(captor.getValue().isAuthor(2L)).isTrue();
    }

    // ── 답변 채택 (핵심: 내공 적립) ────────────────────────────

    @Test
    @DisplayName("acceptAnswer: 질문 작성자가 채택하면 해결처리 + 선생님 내공 10점 적립")
    void acceptAnswer_resolvesAndAwardsNaegong() {
        User student = mockUser(1L);
        User teacher = mockUser(2L);
        QnaQuestion q = question(10L, student, mock(Subject.class));
        QnaAnswer a = answer(20L, q, teacher);
        when(answerRepository.findDetailById(20L)).thenReturn(Optional.of(a));
        when(naegongService.addScore(eq(teacher), eq(10), eq(NaegongReason.ANSWER_ACCEPTED), eq(20L)))
                .thenReturn(130);

        QnaAnswerAcceptResponse res = service.acceptAnswer(1L, 20L);

        assertThat(a.isAccepted()).isTrue();
        assertThat(q.isResolved()).isTrue();
        assertThat(res.isAccepted()).isTrue();
        assertThat(res.questionResolved()).isTrue();
        assertThat(res.addedNaegongScore()).isEqualTo(10);
        assertThat(res.teacherNaegongScore()).isEqualTo(130);
        assertThat(res.teacherUserId()).isEqualTo(2L);
        verify(naegongService).addScore(teacher, 10, NaegongReason.ANSWER_ACCEPTED, 20L);
    }

    @Test
    @DisplayName("acceptAnswer: 질문 작성자가 아니면 QnaForbiddenException, 내공 적립 없음")
    void acceptAnswer_notQuestionAuthor_forbidden() {
        QnaQuestion q = question(10L, mockUser(1L), mock(Subject.class)); // 질문작성자=1
        QnaAnswer a = answer(20L, q, mockUser(2L));
        when(answerRepository.findDetailById(20L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.acceptAnswer(2L, 20L)) // 호출자=2(작성자 아님)
                .isInstanceOf(QnaForbiddenException.class);

        verify(naegongService, never()).addScore(any(), anyInt(), any(), any());
    }

    @Test
    @DisplayName("acceptAnswer: 이미 채택된 질문이면 QnaInvalidStateException(409), 내공 적립 없음")
    void acceptAnswer_alreadyResolved() {
        User student = mockUser(1L);
        QnaQuestion q = question(10L, student, mock(Subject.class));
        q.markResolved(); // 이미 해결됨
        QnaAnswer a = answer(20L, q, mockUser(2L));
        when(answerRepository.findDetailById(20L)).thenReturn(Optional.of(a));

        assertThatThrownBy(() -> service.acceptAnswer(1L, 20L))
                .isInstanceOf(QnaInvalidStateException.class);

        verify(naegongService, never()).addScore(any(), anyInt(), any(), any());
    }

    // ── 좋아요 토글 ───────────────────────────────────────────

    @Test
    @DisplayName("toggleAnswerLike: 좋아요가 없으면 추가하고 likeCount를 올린다")
    void toggleLike_addsWhenAbsent() {
        QnaAnswer a = answer(20L, question(10L, mockUser(1L), mock(Subject.class)), mockUser(2L));
        User liker = mockUser(1L);
        when(answerRepository.findById(20L)).thenReturn(Optional.of(a));
        when(likeRepository.findByAnswerIdAndUserId(20L, 1L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(liker);

        QnaLikeResponse res = service.toggleAnswerLike(1L, 20L);

        assertThat(res.liked()).isTrue();
        assertThat(res.likeCount()).isEqualTo(1);
        assertThat(a.getLikeCount()).isEqualTo(1);
        verify(likeRepository).save(any(QnaAnswerLike.class));
    }

    @Test
    @DisplayName("toggleAnswerLike: 이미 좋아요했으면 취소하고 likeCount를 내린다")
    void toggleLike_removesWhenPresent() {
        QnaAnswer a = answer(20L, question(10L, mockUser(1L), mock(Subject.class)), mockUser(2L));
        a.increaseLikeCount(); // 현재 1
        when(answerRepository.findById(20L)).thenReturn(Optional.of(a));
        when(likeRepository.findByAnswerIdAndUserId(20L, 1L)).thenReturn(Optional.of(mock(QnaAnswerLike.class)));

        QnaLikeResponse res = service.toggleAnswerLike(1L, 20L);

        assertThat(res.liked()).isFalse();
        assertThat(res.likeCount()).isZero();
        verify(likeRepository).delete(any(QnaAnswerLike.class));
    }

    // ── 목록/상세 조회 ────────────────────────────────────────

    @Test
    @DisplayName("getQuestions: 페이지의 질문에 답변 개수를 매핑해 반환한다")
    void getQuestions_mapsAnswerCount() {
        User author = mockUser(1L);
        when(author.getName()).thenReturn("학생");
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(3L);
        when(subject.getName()).thenReturn("수학");
        QnaQuestion q = question(10L, author, subject);

        Page<QnaQuestion> page = new PageImpl<>(List.of(q));
        when(questionRepository.findFiltered(isNull(), isNull(), isNull(), any())).thenReturn(page);
        when(answerRepository.countByQuestionIds(any()))
                .thenReturn(List.of(new QuestionAnswerCount(10L, 2L)));

        Page<QnaQuestionSummaryResponse> res = service.getQuestions(null, null, null, PageRequest.of(0, 10));

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).questionId()).isEqualTo(10L);
        assertThat(res.getContent().get(0).answerCount()).isEqualTo(2L);
        assertThat(res.getContent().get(0).subject().name()).isEqualTo("수학");
    }

    @Test
    @DisplayName("getQuestionDetail: 조회수를 1 증가시키고 상세를 반환한다")
    void getQuestionDetail_increasesViewCount() {
        User author = mockUser(1L);
        when(author.getName()).thenReturn("학생");
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(3L);
        when(subject.getName()).thenReturn("수학");
        QnaQuestion q = question(10L, author, subject);

        when(questionRepository.findDetailById(10L)).thenReturn(Optional.of(q));
        when(questionAttachmentRepository.findByQuestionIdWithFile(10L)).thenReturn(List.of());
        when(answerRepository.findByQuestionIdWithAuthor(10L)).thenReturn(List.of());

        QnaQuestionDetailResponse res = service.getQuestionDetail(10L, null);

        assertThat(res.questionId()).isEqualTo(10L);
        assertThat(res.answers()).isEmpty();
        assertThat(res.images()).isEmpty();
        assertThat(q.getViewCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getQuestionDetail: 존재하지 않으면 QnaQuestionNotFoundException")
    void getQuestionDetail_notFound() {
        when(questionRepository.findDetailById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getQuestionDetail(99L, null))
                .isInstanceOf(QnaQuestionNotFoundException.class);
    }
}
