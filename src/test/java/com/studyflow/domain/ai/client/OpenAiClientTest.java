package com.studyflow.domain.ai.client;

import com.studyflow.domain.ai.exception.AiServiceException;
import com.studyflow.domain.subject.enums.SubjectCategory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * {@link OpenAiClient} 단위 테스트.
 *
 * <p>Spring AI {@link ChatClient}의 호출 체인을 목으로 대체해, 외부 OpenAI 호출 없이
 * (1) 과목별 시스템 프롬프트 구성, (2) 동기 응답 처리, (3) 빈/실패 응답의 예외 변환,
 * (4) 스트리밍 패스스루를 검증한다.</p>
 */
@ExtendWith(MockitoExtension.class)
class OpenAiClientTest {

    @Mock
    ChatClient.Builder builder;
    @Mock
    ChatClient chatClient;
    @Mock
    ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    ChatClient.CallResponseSpec callSpec;
    @Mock
    ChatClient.StreamResponseSpec streamSpec;

    OpenAiClient openAiClient;

    @BeforeEach
    void setUp() {
        when(builder.build()).thenReturn(chatClient);
        openAiClient = new OpenAiClient(builder);
    }

    /** 동기 호출용 fluent 체인(prompt→system→user→call→content) 스텁을 깐다. */
    private void stubCallChain(String answer) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenReturn(answer);
    }

    @Test
    @DisplayName("ask: 과목 지침이 시스템 프롬프트에 포함되고 질문은 user로 전달된다")
    void ask_buildsSubjectAwarePrompt() {
        stubCallChain("정답");

        openAiClient.ask(SubjectCategory.MATH, "2x=4 풀어줘");

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> user = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(requestSpec).system(system.capture());
        org.mockito.Mockito.verify(requestSpec).user(user.capture());

        // 공통 톤 + 수학 분류명 + 수학 지침(검산)이 모두 들어간다
        assertThat(system.getValue())
                .contains("과외 선생님")           // 공통 프롬프트
                .contains("현재 과목: 수학")        // 분류 표시명
                .contains("검산");                 // 수학 전용 지침
        assertThat(user.getValue()).isEqualTo("2x=4 풀어줘");
    }

    @Test
    @DisplayName("ask: category가 null이면 공통 프롬프트만 쓰고 과목 표기는 없다")
    void ask_nullCategoryUsesBaseOnly() {
        stubCallChain("응답");

        openAiClient.ask(null, "질문");

        ArgumentCaptor<String> system = ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(requestSpec).system(system.capture());
        assertThat(system.getValue())
                .contains("과외 선생님")
                .doesNotContain("현재 과목:");
    }

    @Test
    @DisplayName("ask: 앞뒤 공백을 제거한 답변을 반환한다")
    void ask_trimsAnswer() {
        stubCallChain("  답변입니다  ");

        assertThat(openAiClient.ask(SubjectCategory.KOREAN, "질문")).isEqualTo("답변입니다");
    }

    @Test
    @DisplayName("ask: 빈 응답이면 AiServiceException을 던진다")
    void ask_blankAnswerThrows() {
        stubCallChain("   ");

        assertThatThrownBy(() -> openAiClient.ask(SubjectCategory.MATH, "질문"))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    @DisplayName("ask: 호출 중 예외가 나면 AiServiceException으로 변환한다")
    void ask_callFailureWrapped() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callSpec);
        when(callSpec.content()).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> openAiClient.ask(SubjectCategory.MATH, "질문"))
                .isInstanceOf(AiServiceException.class);
    }

    @Test
    @DisplayName("askStream: ChatClient 스트림의 조각들을 그대로 흘려보낸다")
    void askStream_passesThroughChunks() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.stream()).thenReturn(streamSpec);
        when(streamSpec.content()).thenReturn(Flux.just("안녕", "하세요"));

        List<String> chunks = openAiClient.askStream(SubjectCategory.ENGLISH, "hi")
                .collectList()
                .block();

        assertThat(chunks).containsExactly("안녕", "하세요");
    }
}
