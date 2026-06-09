package com.studyflow.domain.qna.dto.request;

/**
 * 본문 블록 한 개(질문·답변 공통). 글과 이미지를 자유롭게 배치하기 위한 단위다.
 *
 * <ul>
 *   <li>{@code type="text"} → {@code text} 사용 (이미지면 null)</li>
 *   <li>{@code type="image"} → {@code fileId} 사용 (먼저 업로드해 받은 FileAsset id)</li>
 * </ul>
 *
 * <p>FE는 블록 배열과 함께 평문 {@code content}(텍스트 블록을 이어붙인 것)도 전송한다 —
 * 목록 미리보기/검색용으로 기존 평문 컬럼을 유지하기 위함이다.</p>
 */
public record QnaBlockRequest(String type, String text, Long fileId) {
}
