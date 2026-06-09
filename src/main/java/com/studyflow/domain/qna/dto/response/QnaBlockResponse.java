package com.studyflow.domain.qna.dto.response;

/**
 * 본문 블록 응답 한 개(질문·답변 공통). 글·이미지를 저장된 순서대로 내려준다.
 *
 * <ul>
 *   <li>{@code type="text"} → {@code text} 채움</li>
 *   <li>{@code type="image"} → {@code fileId}+{@code url} 채움(수정 시 일부만 남기기 위해 fileId 포함)</li>
 * </ul>
 */
public record QnaBlockResponse(String type, String text, Long fileId, String url) {
}
