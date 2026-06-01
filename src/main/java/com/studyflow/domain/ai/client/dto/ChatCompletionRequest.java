package com.studyflow.domain.ai.client.dto;

import java.util.List;

/**
 * OpenAI Chat Completions API 요청 바디.
 * (POST https://api.openai.com/v1/chat/completions)
 *
 * <p>OpenAI가 요구하는 JSON 형식을 그대로 본떠 만든 DTO이다.</p>
 * <pre>
 * {
 *   "model": "gpt-4o-mini",
 *   "messages": [
 *     { "role": "system", "content": "..." },
 *     { "role": "user",   "content": "..." }
 *   ]
 * }
 * </pre>
 *
 * @param model    사용할 모델명 (application.yml의 openai.model)
 * @param messages 대화 메시지 목록 (system 지시 + user 질문)
 */
public record ChatCompletionRequest(
        String model,
        List<Message> messages
) {
    /**
     * 메시지 한 개.
     *
     * @param role    "system" | "user" | "assistant"
     * @param content 메시지 본문
     */
    public record Message(String role, String content) {
        public static Message system(String content) { return new Message("system", content); }
        public static Message user(String content)   { return new Message("user", content); }
    }
}
