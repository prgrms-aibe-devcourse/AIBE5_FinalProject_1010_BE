package com.studyflow.domain.chat.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebSocket 채팅 처리 중 발생한 에러를 클라이언트에게 전달하는 응답.
 *
 * 메시지 전송/읽음 처리 중 예외가 나면 메시지가 조용히 사라지는 대신
 * 에러를 요청한 사용자 본인에게만 전달한다.
 *
 * 클라이언트 구독 경로:
 * /user/sub/errors
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatErrorResponse {

    /**
     * 에러가 발생한 채팅방 ID.
     *
     * 어떤 방에서 발생한 에러인지 알 수 없으면 null.
     */
    private Long roomId;

    /**
     * 사용자에게 보여줄 에러 메시지.
     */
    private String message;
}
