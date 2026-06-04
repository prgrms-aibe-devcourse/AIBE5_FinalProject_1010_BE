package com.studyflow.domain.chat.dto.response;

import com.studyflow.domain.chat.enums.ChatCallSignalType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 같은 채팅방 참여자에게 브로드캐스트되는 보이스톡 신호.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCallSignalResponse {

    private Long roomId;

    private String callId;

    private ChatCallSignalType type;

    private Long senderId;

    private Long targetUserId;

    private Map<String, Object> sdp;

    private Map<String, Object> iceCandidate;

    private String reason;

    private LocalDateTime sentAt;
}
