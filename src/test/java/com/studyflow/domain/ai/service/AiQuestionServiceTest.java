package com.studyflow.domain.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.studyflow.domain.ai.client.OpenAiClient;
import com.studyflow.domain.ai.dto.request.AiQuestionCreateRequest;
import com.studyflow.domain.ai.dto.response.AiQuestionResponse;
import com.studyflow.domain.ai.entity.AiQuestion;
import com.studyflow.domain.ai.exception.AiQuestionNotFoundException;
import com.studyflow.domain.ai.exception.SubjectNotFoundException;
import com.studyflow.domain.ai.repository.AiQuestionRepository;
import com.studyflow.domain.file.entity.FileAsset;
import com.studyflow.domain.file.repository.FileAssetRepository;
import com.studyflow.domain.subject.entity.Subject;
import com.studyflow.domain.subject.enums.SubjectCategory;
import com.studyflow.domain.subject.repository.SubjectRepository;
import com.studyflow.domain.user.entity.User;
import com.studyflow.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link AiQuestionService} 단위 테스트.
 *
 * <p>리포지토리·OpenAI 클라이언트를 목으로 대체해, 검증→호출→저장→응답 흐름과
 * 스트리밍(SSE) 이벤트 구성을 외부 의존 없이 확인한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class AiQuestionServiceTest {

    @Mock
    AiQuestionRepository aiQuestionRepository;
    @Mock
    SubjectRepository subjectRepository;
    @Mock
    UserRepository userRepository;
    @Mock
    FileAssetRepository fileAssetRepository;
    @Mock
    OpenAiClient openAiClient;

    AiQuestionService service;

    @BeforeEach
    void setUp() {
        // ObjectMapper는 done 이벤트 직렬화에 실제로 쓰이므로 진짜 인스턴스를 주입한다.
        service = new AiQuestionService(
                aiQuestionRepository, subjectRepository, userRepository,
                fileAssetRepository, openAiClient, new ObjectMapper()
        );
    }

    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private Subject mockSubject(Long id, String name, SubjectCategory category) {
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(id);
        when(subject.getName()).thenReturn(name);
        when(subject.getCategory()).thenReturn(category);
        return subject;
    }

    // ── 동기 ask ─────────────────────────────────────────────

    @Test
    @DisplayName("ask: 과목 category로 OpenAI를 호출하고, 질문+답변을 저장한 뒤 응답을 만든다")
    void ask_happyPath() {
        User user = mockUser(1L);
        Subject subject = mockSubject(3L, "수학", SubjectCategory.MATH);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(subject));
        when(openAiClient.ask(SubjectCategory.MATH, "2x=4")).thenReturn("x=2 입니다");
        when(aiQuestionRepository.save(any(AiQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        AiQuestionResponse response = service.ask(1L, new AiQuestionCreateRequest(3L, "2x=4", null));

        // OpenAI는 과목 category와 함께 호출되었다
        verify(openAiClient).ask(SubjectCategory.MATH, "2x=4");
        // 저장된 엔티티의 본문/답변이 정확하다
        ArgumentCaptor<AiQuestion> saved = ArgumentCaptor.forClass(AiQuestion.class);
        verify(aiQuestionRepository).save(saved.capture());
        assertThat(saved.getValue().getQuestionText()).isEqualTo("2x=4");
        assertThat(saved.getValue().getAnswerText()).isEqualTo("x=2 입니다");
        // 응답 DTO가 과목 이름까지 담는다
        assertThat(response.subjectId()).isEqualTo(3L);
        assertThat(response.subjectName()).isEqualTo("수학");
        assertThat(response.answerText()).isEqualTo("x=2 입니다");
        assertThat(response.questionImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("ask: 존재하지 않는 과목이면 SubjectNotFoundException, OpenAI 호출/저장은 없다")
    void ask_subjectNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ask(1L, new AiQuestionCreateRequest(99L, "q", null)))
                .isInstanceOf(SubjectNotFoundException.class);

        verify(openAiClient, never()).ask(any(), any());
        verify(aiQuestionRepository, never()).save(any());
    }

    @Test
    @DisplayName("ask: 존재하지 않는 사용자면 IllegalArgumentException")
    void ask_userNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.ask(1L, new AiQuestionCreateRequest(3L, "q", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(openAiClient, never()).ask(any(), any());
    }

    @Test
    @DisplayName("ask: 첨부 이미지가 본인 소유가 아니면 거부하고 저장하지 않는다")
    void ask_rejectsForeignImage() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(subjectRepository.findById(3L)).thenReturn(Optional.of(mock(Subject.class)));

        FileAsset foreign = mock(FileAsset.class);
        when(foreign.getId()).thenReturn(12L);
        User otherOwner = mock(User.class);
        when(otherOwner.getId()).thenReturn(999L);  // 다른 사람이 올린 파일
        when(foreign.getUploader()).thenReturn(otherOwner);
        when(fileAssetRepository.findByIdInWithUploader(List.of(12L))).thenReturn(List.of(foreign));

        assertThatThrownBy(() ->
                service.ask(1L, new AiQuestionCreateRequest(3L, "q", List.of(12L))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(openAiClient, never()).ask(any(), any());
        verify(aiQuestionRepository, never()).save(any());
    }

    // ── 스트리밍 askStream ───────────────────────────────────

    @Test
    @DisplayName("askStream: 토큰을 data 이벤트로 흘려보내고, 종료 후 누적 답변을 저장하며 done 이벤트를 낸다")
    void askStream_streamsThenPersists() {
        User user = mockUser(1L);
        Subject subject = mockSubject(1L, "국어", SubjectCategory.KOREAN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(subject));
        when(openAiClient.askStream(SubjectCategory.KOREAN, "정서?"))
                .thenReturn(Flux.just("고독", "과 그리움"));
        when(aiQuestionRepository.save(any(AiQuestion.class))).thenAnswer(inv -> inv.getArgument(0));

        List<ServerSentEvent<String>> events =
                service.askStream(1L, new AiQuestionCreateRequest(1L, "정서?", null))
                        .collectList()
                        .block();

        assertThat(events).hasSize(3);
        // 1~2번째: 토큰 data 이벤트(이벤트명 없음)
        assertThat(events.get(0).data()).isEqualTo("고독");
        assertThat(events.get(0).event()).isNull();
        assertThat(events.get(1).data()).isEqualTo("과 그리움");
        // 3번째: done 이벤트 + 저장된 기록 JSON
        assertThat(events.get(2).event()).isEqualTo("done");
        assertThat(events.get(2).data())
                .contains("\"subjectName\":\"국어\"")
                .contains("\"answerText\":\"고독과 그리움\"");

        // 누적 답변이 통째로 저장되었다
        ArgumentCaptor<AiQuestion> saved = ArgumentCaptor.forClass(AiQuestion.class);
        verify(aiQuestionRepository).save(saved.capture());
        assertThat(saved.getValue().getAnswerText()).isEqualTo("고독과 그리움");
        assertThat(saved.getValue().getQuestionText()).isEqualTo("정서?");
    }

    @Test
    @DisplayName("askStream: 잘못된 과목이면 스트림을 만들기 전에 예외(일반 HTTP 에러로 응답)")
    void askStream_subjectNotFoundFailsFast() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mock(User.class)));
        when(subjectRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.askStream(1L, new AiQuestionCreateRequest(99L, "q", null)))
                .isInstanceOf(SubjectNotFoundException.class);

        verify(openAiClient, never()).askStream(any(), any());
    }

    // ── 상세 조회 getDetail ─────────────────────────

    @Test
    @DisplayName("getDetail: 본인 기록이면 질문+답변 전체를 반환한다")
    void getDetail_returnsFullRecordForOwner() {
        User user = mockUser(1L);
        // getDetail은 과목의 category를 쓰지 않으므로 필요한 stub만 둔다(불필요 stub 방지).
        Subject subject = mock(Subject.class);
        when(subject.getId()).thenReturn(3L);
        when(subject.getName()).thenReturn("수학");
        AiQuestion question = AiQuestion.create(user, subject, "2x=4?", "x=2 입니다");
        when(aiQuestionRepository.findById(5L)).thenReturn(Optional.of(question));

        AiQuestionResponse response = service.getDetail(1L, 5L);

        assertThat(response.questionText()).isEqualTo("2x=4?");
        assertThat(response.answerText()).isEqualTo("x=2 입니다");
        assertThat(response.subjectName()).isEqualTo("수학");
    }

    @Test
    @DisplayName("getDetail: 타인 기록이면 AiQuestionNotFoundException(404)")
    void getDetail_rejectsOtherUsersRecord() {
        User owner = mock(User.class);
        when(owner.getId()).thenReturn(999L);  // 다른 사람의 기록
        AiQuestion question = AiQuestion.create(owner, mock(Subject.class), "q", "a");
        when(aiQuestionRepository.findById(5L)).thenReturn(Optional.of(question));

        assertThatThrownBy(() -> service.getDetail(1L, 5L))
                .isInstanceOf(AiQuestionNotFoundException.class);
    }

    @Test
    @DisplayName("getDetail: 존재하지 않으면 AiQuestionNotFoundException(404)")
    void getDetail_notFound() {
        when(aiQuestionRepository.findById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getDetail(1L, 5L))
                .isInstanceOf(AiQuestionNotFoundException.class);
    }

    // ── 기록 조회 getMyHistory ──────────────────────────────

    @Test
    @DisplayName("getMyHistory: 내 userId로 페이징 조회를 위임한다")
    void getMyHistory_delegatesToRepository() {
        when(aiQuestionRepository.findHistoryByUserId(eq(1L), any()))
                .thenReturn(org.springframework.data.domain.Page.empty());

        service.getMyHistory(1L, org.springframework.data.domain.PageRequest.of(0, 20));

        verify(aiQuestionRepository).findHistoryByUserId(eq(1L), any());
    }
}
