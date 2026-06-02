package com.studyflow.domain.ai.client;

import com.studyflow.domain.ai.exception.AiServiceException;
import com.studyflow.domain.subject.enums.SubjectCategory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * OpenAI(LLM) 호출 클라이언트 계층.
 *
 * <p>이 프로젝트는 별도 Python(FastAPI) AI 서버를 두지 않고, Spring에서 OpenAI를 직접
 * 호출한다. 호출은 Spring AI의 {@link ChatClient}로 통일했다(이전의 커스텀 RestClient
 * 방식 대체). 모델·API 키 등은 {@code spring.ai.openai.*} 프로퍼티로 자동 구성된 빈을
 * 그대로 사용하므로, 이 클래스는 "시스템 프롬프트 구성 + 호출"에만 집중한다.</p>
 *
 * <p>두 가지 호출 방식을 제공한다.</p>
 * <ul>
 *   <li>{@link #ask(SubjectCategory, String)} — 답변 전체를 한 번에 받는 동기 호출</li>
 *   <li>{@link #askStream(SubjectCategory, String)} — 답변을 토큰 단위로 흘려보내는 스트리밍 호출</li>
 * </ul>
 */
@Slf4j
@Component
public class OpenAiClient {

    /** 모든 과목에 공통으로 적용되는 기본 시스템 프롬프트(과외 도우미 톤). */
    private static final String BASE_SYSTEM_PROMPT = """
            당신은 한국 고등학생의 수능·내신 학습을 돕는 친절한 과외 선생님입니다.
            학생의 질문에 대해 정답만 알려주지 말고, 풀이 과정을 단계별로 쉽게 설명하세요.
            수식은 보기 쉽게 작성하고, 한국어로 답변하세요.
            """;

    private final ChatClient chatClient;

    public OpenAiClient(ChatClient.Builder chatClientBuilder) {
        // Spring AI가 자동 구성한 ChatClient.Builder(OpenAiChatModel 기반)로 클라이언트를 만든다.
        // 모델·온도 등 옵션은 application.yml의 spring.ai.openai.* 에서 적용된다.
        this.chatClient = chatClientBuilder.build();
    }

    /**
     * 질문 텍스트를 과목에 맞춰 OpenAI에 보내 답변을 한 번에 받는다.(동기)
     *
     * @param category     질문 과목 대분류 (null이면 공통 프롬프트만 사용)
     * @param questionText 학생이 입력한 질문
     * @return AI가 생성한 답변 텍스트
     * @throws AiServiceException 호출 실패 또는 빈 응답일 때
     */
    public String ask(SubjectCategory category, String questionText) {
        try {
            String answer = chatClient.prompt()
                    .system(buildSystemPrompt(category))
                    .user(questionText)
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                throw new AiServiceException("AI 응답이 비어 있습니다.");
            }
            return answer.trim();

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            // 네트워크 오류, 4xx/5xx, 타임아웃 등 호출 단계의 모든 실패를 여기서 잡는다.
            log.error("OpenAI 호출 실패", e);
            throw new AiServiceException("AI 풀이 요청에 실패했습니다.", e);
        }
    }

    /**
     * 질문 텍스트를 과목에 맞춰 OpenAI에 보내 답변을 토큰 단위로 스트리밍한다.
     *
     * <p>반환된 {@link Flux}는 모델이 생성하는 답변 조각(델타)을 순서대로 방출한다.
     * 호출 측(서비스)에서 이를 SSE로 클라이언트에 흘려보내고, 스트림이 끝나면 전체 답변을
     * 이어붙여 저장한다.</p>
     *
     * @param category     질문 과목 대분류 (null이면 공통 프롬프트만 사용)
     * @param questionText 학생이 입력한 질문
     * @return 답변 조각들의 스트림
     */
    public Flux<String> askStream(SubjectCategory category, String questionText) {
        return chatClient.prompt()
                .system(buildSystemPrompt(category))
                .user(questionText)
                .stream()
                .content();
    }

    /**
     * 공통 프롬프트에 과목별 지침을 덧붙여 최종 시스템 프롬프트를 만든다.
     * category가 null(대분류가 지정되지 않은 과목)이면 공통 프롬프트만 사용한다.
     */
    private String buildSystemPrompt(SubjectCategory category) {
        if (category == null) {
            return BASE_SYSTEM_PROMPT;
        }
        return BASE_SYSTEM_PROMPT
                + "\n[현재 과목: " + category.getDisplayName() + "]\n"
                + category.getTutorGuidance();
    }
}
