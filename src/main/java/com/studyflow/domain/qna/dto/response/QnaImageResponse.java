package com.studyflow.domain.qna.dto.response;

/**
 * 첨부 이미지 한 장. fileId와 표시용 url을 함께 내려준다.
 *
 * <p>수정 화면에서 기존 이미지를 일부만 남기고 일부만 삭제하려면 각 이미지의 fileId가 필요하다.
 * (FE가 "남길 fileId들 + 새로 올린 fileId들"을 합쳐 imageFileIds로 다시 보낸다.)</p>
 */
public record QnaImageResponse(Long fileId, String url) {
}
