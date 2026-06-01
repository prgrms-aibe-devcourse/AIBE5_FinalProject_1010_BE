package com.studyflow.domain.ai.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * OpenAI Chat Completions API 응답 바디(필요한 부분만 매핑).
 *
 * <p>실제 응답에는 usage, id, created 등 더 많은 필드가 있지만, 우리가 쓰는 것은
 * choices[0].message.content 뿐이다. 나머지는 {@link JsonIgnoreProperties}로 무시한다.</p>
 * <pre>
 * {
 *   "choices": [
 *     { "message": { "role": "assistant", "content": "정답은 ..." } }
 *   ]
 * }
 * </pre>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ChatCompletionResponse(
        List<Choice> choices
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(Message message) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Message(String role, String content) {}

    /**
     * 첫 번째 선택지의 답변 텍스트를 꺼낸다.
     *
     * @return 답변 본문, 없으면 null
     */
    public String firstAnswer() {
        if (choices == null || choices.isEmpty()) return null;
        Choice first = choices.get(0);
        if (first == null || first.message() == null) return null;
        return first.message().content();
    }
}
