package com.studyflow.domain.qna.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 질문 작성 요청. (POST /api/v1/qna/questions, STUDENT)
 *
 * <p>첨부 이미지는 먼저 파일 업로드 API로 올려 받은 fileId 목록({@code imageFileIds})으로 전달한다.
 * 한 질문에 여러 장 첨부 가능. (AI 질문과 동일한 컨벤션)</p>
 */
@Getter
@NoArgsConstructor
public class QnaQuestionCreateRequest {

    @NotNull(message = "과목은 필수입니다.")
    private Long subjectId;

    @NotBlank(message = "제목은 필수입니다.")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    // 첨부 이미지들의 FileAsset id 목록 (선택, 없으면 null 또는 빈 배열)
    private List<Long> imageFileIds;

    // 글·이미지를 자유롭게 배치한 본문 블록(선택). 주면 이미지 첨부는 블록의 image 블록에서 도출한다.
    private List<QnaBlockRequest> blocks;
}
