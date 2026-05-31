package com.studyflow.domain.ai.client;

import com.studyflow.domain.ai.client.dto.ChatCompletionRequest;
import com.studyflow.domain.ai.client.dto.ChatCompletionResponse;
import com.studyflow.domain.ai.exception.AiServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * OpenAI(LLM)를 직접 호출하는 클라이언트 계층.
 *
 * <p>이 프로젝트는 별도 Python(FastAPI) AI 서버를 두지 않고, Spring에서 OpenAI
 * Chat Completions API를 직접 호출한다. 그 HTTP 호출의 세부사항(엔드포인트, 요청/응답
 * 형식, 에러 변환)을 이 클래스 한 곳에 모아, 서비스 계층은 {@link #ask(String)} 한 줄로
 * AI 답변을 얻게 한다.</p>
 *
 * <p>1단계에서는 텍스트 질문만 처리한다. 2단계(이미지)·3단계(음성)에서
 * 메시지 content를 멀티모달 형식으로 확장하면 된다.</p>
 */
@Slf4j
@Component
public class OpenAiClient {

    /** 모델에게 역할을 알려주는 시스템 프롬프트(과외 도우미 톤). */
    private static final String SYSTEM_PROMPT = """
            당신은 한국 초·중·고 학생을 돕는 친절한 과외 선생님입니다.
            학생의 질문에 대해 정답만 알려주지 말고, 풀이 과정을 단계별로 쉽게 설명하세요.
            수식은 보기 쉽게 작성하고, 한국어로 답변하세요.
            """;

    private final RestClient openAiRestClient;
    private final String model;

    public OpenAiClient(
            // OpenAiConfig에서 등록한 OpenAI 전용 RestClient를 주입받는다.
            @Qualifier("openAiRestClient") RestClient openAiRestClient,
            @Value("${openai.model}") String model
    ) {
        this.openAiRestClient = openAiRestClient;
        this.model = model;
    }

    /**
     * 질문 텍스트를 OpenAI에 보내 답변을 받는다.
     *
     * @param questionText 학생이 입력한 질문
     * @return AI가 생성한 답변 텍스트
     * @throws AiServiceException 호출 실패 또는 빈 응답일 때
     */
    public String ask(String questionText) {
        ChatCompletionRequest request = new ChatCompletionRequest(
                model,
                List.of(
                        ChatCompletionRequest.Message.system(SYSTEM_PROMPT),
                        ChatCompletionRequest.Message.user(questionText)
                )
        );

        try {
            ChatCompletionResponse response = openAiRestClient.post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(ChatCompletionResponse.class);

            String answer = (response == null) ? null : response.firstAnswer();
            if (answer == null || answer.isBlank()) {
                throw new AiServiceException("AI 응답이 비어 있습니다.");
            }
            return answer.trim();

        } catch (RestClientException e) {
            // 네트워크 오류, 4xx/5xx, 타임아웃 등 HTTP 호출 단계의 모든 실패를 여기서 잡는다.
            log.error("OpenAI 호출 실패", e);
            throw new AiServiceException("AI 풀이 요청에 실패했습니다.", e);
        }
    }
}
