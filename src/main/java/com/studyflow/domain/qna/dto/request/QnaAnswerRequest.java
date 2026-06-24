package com.studyflow.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 답변 작성/수정 요청. (POST /api/v1/qna/questions/{questionId}/answers, TEACHER)
 *
 * <p>{@code imageFileIds}는 첨부 이미지 fileId 목록(여러 장 가능). 수정 시에는 최종 첨부 상태(전체 교체).</p>
 */
@Getter
@NoArgsConstructor
public class QnaAnswerRequest {

    @NotBlank(message = "답변 내용은 필수입니다.")
    private String content;

    private List<Long> imageFileIds;

    // 글·이미지를 자유롭게 배치한 본문 블록(선택). 주면 이미지 첨부는 블록에서 도출한다.
    private List<QnaBlockRequest> blocks;

    /** 테스트 전용 — 실제 API 요청은 Jackson 역직렬화로 처리되므로 서비스 코드에서 직접 호출하지 않습니다. */
    public static QnaAnswerRequest of(String content) {
        QnaAnswerRequest req = new QnaAnswerRequest();
        req.content = content;
        return req;
    }
}
